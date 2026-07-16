/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.lower.AbstractSuspendFunctionsLowering.Companion.DECLARATION_ORIGIN_COROUTINE_IMPL
import org.jetbrains.kotlin.backend.common.lower.AbstractSuspendFunctionsLowering.Companion.DECLARATION_ORIGIN_COROUTINE_IMPL_INVOKE
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.wasm.WasmBackendContext
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrRawFunctionReferenceImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrTypeOperatorCallImpl
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.Name

/**
 * Merges structurally identical suspend lambda coroutine classes into shared classes
 * that dispatch through a `wasmFuncRef`.
 *
 * Before this pass, each suspend lambda produces its own coroutine class:
 * ```
 * class $useSuspendLambda1$slambda$invokeCOROUTINE$ : CoroutineImplStateMachine {
 *     var $result: Any?           // captured SharedVariableBox
 *     var tmp00: SharedVariableBox // spilled local
 *     override fun doResume(): Any? { /* state machine */ }
 * }
 * ```
 *
 * After this pass, structurally identical classes share one class:
 * ```
 * class SuspendLambda_1A_0(
 *     func: WasmTypedFuncRef,     // doResume implementation
 *     p0: Any?,                   // captured value
 *     completion: Continuation
 * ) : CoroutineImplStateMachine(completion) {
 *     override fun doResume(): Any? = callRef(func, this)
 * }
 *
 * // Per-lambda top-level function:
 * fun $useSuspendLambda1$doResume(self: SuspendLambda_1A_0): Any? {
 *     // original state machine body
 * }
 * ```
 *
 * We maintain a file local class cache in this pass and rely on the linker to deduplicate
 * cross-file classes and types.
 *
 * The replaced classes are removed from the IR.
 *
 */
internal class WasmSuspendLambdaMergingLowering(val context: WasmBackendContext) : FileLoweringPass {

    companion object {
        val SUSPEND_LAMBDA_MERGING_CLASS by IrDeclarationOriginImpl.Regular
    }

    private val fileLocalClassCache = mutableMapOf<String, SharedClassInfo>()

    private data class SharedClassInfo(
        val irClass: IrClass,
        val funcField: IrField,
        val capturedFields: List<IrField>,
    )

    private data class ClassReplacement(
        val sharedClassInfo: SharedClassInfo,
        val bridgedFunction: IrSimpleFunction,
        val originalConstructor: IrConstructor,
        val originalClass: IrClass,
    )

    override fun lower(irFile: IrFile) {
        if (context.wasmUseStackSwitching) return

        fileLocalClassCache.clear()

        val coroutineImplClass = context.wasmSymbols.coroutineImpl.owner

        val suspendLambdaClasses = irFile.declarations
            .filterIsInstance<IrClass>()
            .filter { isSuspendLambdaCoroutineClass(it, coroutineImplClass) }

        if (suspendLambdaClasses.isEmpty()) return

        val classReplacements = mutableMapOf<IrClassSymbol, ClassReplacement>()

        for (originalClass in suspendLambdaClasses) {
            val replacement = processCoroutineClass(originalClass, irFile) ?: continue
            classReplacements[originalClass.symbol] = replacement
        }

        if (classReplacements.isEmpty()) return

        irFile.transform(ConstructorCallReplacer(classReplacements), null)

        val replacedClasses = classReplacements.values.mapTo(HashSet()) { it.originalClass }
        irFile.declarations.removeAll(replacedClasses)
    }

    private fun isSuspendLambdaCoroutineClass(irClass: IrClass, coroutineImplClass: IrClass): Boolean {
        if (irClass.origin != DECLARATION_ORIGIN_COROUTINE_IMPL) return false
        return irClass.superTypes.any { it.classOrNull == coroutineImplClass.symbol }
    }

    private fun processCoroutineClass(originalClass: IrClass, irFile: IrFile): ClassReplacement? {
        val doResumeMethod = originalClass.simpleFunctions()
            .firstOrNull { it.origin == DECLARATION_ORIGIN_COROUTINE_IMPL_INVOKE }
            ?: return null

        val originalConstructor = originalClass.primaryConstructor ?: return null

        val extraFields = coroutineClassOwnFields(originalClass)

        val key = computeKey(extraFields)

        val stageController = context.irFactory.stageController
        val sharedInfo = fileLocalClassCache.getOrPut(key) {
            stageController.restrictTo(originalClass) {
                buildSharedCoroutineClass(key, extraFields, irFile)
            }
        }

        val bridgedFunction = stageController.restrictTo(originalClass) {
            buildBridgedDoResume(originalClass, doResumeMethod, sharedInfo, irFile)
        }

        return ClassReplacement(
            sharedClassInfo = sharedInfo,
            bridgedFunction = bridgedFunction,
            originalConstructor = originalConstructor,
            originalClass = originalClass,
        )
    }

    /**
     * The direct declared fields of the coroutine class (i.e. captured values and spills).
     *
     */
    private fun coroutineClassOwnFields(irClass: IrClass): List<IrField> =
        irClass.declarations.filterIsInstance<IrField>()

    private fun computeKey(extraFields: List<IrField>): String {
        val anyNType = context.irBuiltIns.anyNType
        val fieldTypeCodes = extraFields.joinToString("") { it.type.eraseIfReferenceType(anyNType).toTypeSignatureCode() }
        return "SuspendLambda_${extraFields.size}${fieldTypeCodes}"
    }

    private fun buildSharedCoroutineClass(
        key: String,
        templateFields: List<IrField>,
        irFile: IrFile,
    ): SharedClassInfo {
        val anyNType = context.irBuiltIns.anyNType
        val coroutineImplClass = context.wasmSymbols.coroutineImpl.owner
        val coroutineBaseConstructor = coroutineImplClass.constructors.single { it.hasShape(regularParameters = 1) }

        val sharedClass = context.irFactory.buildClass {
            startOffset = SYNTHETIC_OFFSET
            endOffset = SYNTHETIC_OFFSET
            origin = SUSPEND_LAMBDA_MERGING_CLASS
            name = Name.identifier(key)
            visibility = DescriptorVisibilities.PUBLIC
        }.apply {
            parent = irFile
            irFile.declarations += this
            createThisReceiverParameter()
            superTypes = listOf(coroutineImplClass.defaultType)
        }

        val doResumeFuncType = context.irBuiltIns.functionN(1).typeWith(coroutineImplClass.defaultType, anyNType)

        val funcField = sharedClass.addField {
            startOffset = SYNTHETIC_OFFSET
            endOffset = SYNTHETIC_OFFSET
            name = Name.identifier("func")
            visibility = DescriptorVisibilities.PRIVATE
            isFinal = true
            type = context.wasmSymbols.wasmTypedFuncRefType(doResumeFuncType)
        }

        val capturedFields = templateFields.mapIndexed { index, templateField ->
            sharedClass.addField {
                startOffset = SYNTHETIC_OFFSET
                endOffset = SYNTHETIC_OFFSET
                name = Name.identifier("f\$$index")
                visibility = DescriptorVisibilities.PRIVATE
                isFinal = false
                type = templateField.type.eraseIfReferenceType(anyNType)
            }
        }

        val constructor = sharedClass.addConstructor {
            origin = DECLARATION_ORIGIN_COROUTINE_IMPL
            isPrimary = true
            startOffset = SYNTHETIC_OFFSET
            endOffset = SYNTHETIC_OFFSET
        }.apply {
            parameters = buildList {
                add(buildValueParameter(this@apply) {
                    name = Name.identifier("func")
                    startOffset = SYNTHETIC_OFFSET
                    endOffset = SYNTHETIC_OFFSET
                    type = funcField.type
                    kind = IrParameterKind.Regular
                })
                capturedFields.forEachIndexed { index, field ->
                    add(buildValueParameter(this@apply) {
                        name = Name.identifier("p$index")
                        startOffset = SYNTHETIC_OFFSET
                        endOffset = SYNTHETIC_OFFSET
                        type = field.type
                        kind = IrParameterKind.Regular
                    })
                }
                add(buildValueParameter(this@apply) {
                    name = Name.identifier("completion")
                    startOffset = SYNTHETIC_OFFSET
                    endOffset = SYNTHETIC_OFFSET
                    type = coroutineBaseConstructor.parameters[0].type
                    kind = IrParameterKind.Regular
                })
            }

            val completionParam = parameters.last()
            val funcParam = parameters[0]
            val irBuilder = context.createIrBuilder(symbol, SYNTHETIC_OFFSET, SYNTHETIC_OFFSET)
            body = irBuilder.irBlockBody {
                +irDelegatingConstructorCall(coroutineBaseConstructor).apply {
                    arguments[0] = irGet(completionParam)
                }
                +irSetField(irGet(sharedClass.thisReceiver!!), funcField, irGet(funcParam))
                capturedFields.forEachIndexed { index, field ->
                    +irSetField(irGet(sharedClass.thisReceiver!!), field, irGet(parameters[index + 1]))
                }
            }
        }

        val superDoResume = coroutineImplClass.simpleFunctions()
            .single { it.name.asString() == "doResume" }

        sharedClass.addFunction {
            startOffset = SYNTHETIC_OFFSET
            endOffset = SYNTHETIC_OFFSET
            origin = DECLARATION_ORIGIN_COROUTINE_IMPL_INVOKE
            name = Name.identifier("doResume")
            returnType = anyNType
            modality = Modality.FINAL
        }.apply {
            parameters = listOf(createDispatchReceiverParameterWithClassParent()) +
                    superDoResume.nonDispatchParameters.map { it.copyTo(this, DECLARATION_ORIGIN_COROUTINE_IMPL) }
            overriddenSymbols = listOf(superDoResume.symbol)

            val callRefSymbol = context.wasmSymbols.callRef
            val builder = context.createIrBuilder(symbol, SYNTHETIC_OFFSET, SYNTHETIC_OFFSET)
            body = builder.irBlockBody {
                +irReturn(
                    irCall(callRefSymbol).apply {
                        typeArguments[0] = anyNType
                        arguments[0] = irGetField(irGet(dispatchReceiverParameter!!), funcField)
                        arguments.add(IrTypeOperatorCallImpl(
                            SYNTHETIC_OFFSET, SYNTHETIC_OFFSET,
                            coroutineImplClass.defaultType, IrTypeOperator.REINTERPRET_CAST,
                            coroutineImplClass.defaultType, irGet(dispatchReceiverParameter!!)
                        ))
                    }
                )
            }
        }

        sharedClass.addFakeOverrides(context.typeSystem)

        return SharedClassInfo(sharedClass, funcField, capturedFields)
    }

    private fun buildBridgedDoResume(
        originalClass: IrClass,
        originalDoResume: IrSimpleFunction,
        sharedInfo: SharedClassInfo,
        irFile: IrFile,
    ): IrSimpleFunction {
        val anyNType = context.irBuiltIns.anyNType

        val bridgedName = buildString {
            val parent = originalClass.parent
            if (parent is IrDeclarationWithName) {
                append(parent.name.asString())
                append('$')
            }
            append(originalClass.name.asString())
            append("\$doResume")
        }

        return context.irFactory.addFunction(irFile) {
            startOffset = originalDoResume.startOffset
            endOffset = originalDoResume.endOffset
            origin = IrDeclarationOrigin.DEFINED
            name = Name.identifier(bridgedName)
            returnType = anyNType
        }.apply {
            val selfParam = buildValueParameter(this) {
                name = Name.identifier("self")
                startOffset = SYNTHETIC_OFFSET
                endOffset = SYNTHETIC_OFFSET
                type = context.wasmSymbols.coroutineImpl.owner.defaultType
                kind = IrParameterKind.Regular
            }
            parameters = listOf(selfParam)

            val originalDispatch = originalDoResume.dispatchReceiverParameter ?: return@apply

            val originalBody = originalDoResume.body ?: return@apply

            val originalExtraFields = coroutineClassOwnFields(originalClass)
            check(originalExtraFields.size == sharedInfo.capturedFields.size) {
                "Shared class ${sharedInfo.irClass.name} has ${sharedInfo.capturedFields.size} captured fields, " +
                        "but ${originalClass.name} declares ${originalExtraFields.size}; " +
                        "every declared field must be mapped or its accesses would keep the original class type"
            }

            val fieldMapping = originalExtraFields.zip(sharedInfo.capturedFields).toMap()
            val originalFieldTypes = mutableMapOf<IrField, IrType>()
            originalExtraFields.zip(sharedInfo.capturedFields).forEach { pair ->
                originalFieldTypes[pair.second] = pair.first.type
            }

            val clonedBody = originalBody.deepCopyWithSymbols(this)

            val sharedClassType = sharedInfo.irClass.defaultType
            val refCastNullSymbol = context.wasmSymbols.refCastNull

            clonedBody.transformChildrenVoid(object : IrElementTransformerVoid() {
                override fun visitGetValue(expression: IrGetValue): IrExpression {
                    if (expression.symbol == originalDispatch.symbol) {
                        val builder = context.createIrBuilder(this@apply.symbol, expression.startOffset, expression.endOffset)
                        return IrTypeOperatorCallImpl(
                            expression.startOffset, expression.endOffset,
                            sharedClassType, IrTypeOperator.IMPLICIT_CAST, sharedClassType,
                            builder.irGet(selfParam)
                        )
                    }
                    return super.visitGetValue(expression)
                }

                override fun visitGetField(expression: IrGetField): IrExpression {
                    val mapped = fieldMapping[expression.symbol.owner]
                    if (mapped != null) {
                        val get = context.createIrBuilder(this@apply.symbol, expression.startOffset, expression.endOffset)
                            .irGetField(
                                expression.receiver?.transform(this, null),
                                mapped
                            )
                        val originalType = originalFieldTypes[mapped]
                        if (originalType != null && originalType != mapped.type) {
                            return IrTypeOperatorCallImpl(
                                expression.startOffset, expression.endOffset,
                                originalType, IrTypeOperator.IMPLICIT_CAST, originalType, get
                            )
                        }
                        return get
                    }
                    return super.visitGetField(expression)
                }

                override fun visitSetField(expression: IrSetField): IrExpression {
                    val mapped = fieldMapping[expression.symbol.owner]
                    if (mapped != null) {
                        return context.createIrBuilder(this@apply.symbol, expression.startOffset, expression.endOffset)
                            .irSetField(
                                expression.receiver?.transform(this, null),
                                mapped,
                                expression.value.transform(this, null)
                            )
                    }
                    return super.visitSetField(expression)
                }

                override fun visitReturn(expression: IrReturn): IrExpression {
                    if (expression.returnTargetSymbol == originalDoResume.symbol) {
                        expression.returnTargetSymbol = this@apply.symbol
                    }
                    return super.visitReturn(expression)
                }

                override fun visitDeclaration(declaration: IrDeclarationBase): IrStatement {
                    if (declaration.parent == originalDoResume)
                        declaration.parent = this@apply
                    return super.visitDeclaration(declaration)
                }
            })

            body = clonedBody
        }
    }

    private inner class ConstructorCallReplacer(
        private val replacements: Map<IrClassSymbol, ClassReplacement>,
    ) : IrElementTransformerVoid() {

        private fun remapType(type: IrType): IrType {
            val classSymbol = type.classOrNull ?: return type
            val replacement = replacements[classSymbol] ?: return type
            return replacement.sharedClassInfo.irClass.defaultType
        }

        override fun visitVariable(declaration: IrVariable): IrStatement {
            val result = super.visitVariable(declaration)
            if (result is IrVariable) {
                result.type = remapType(result.type)
            }
            return result
        }

        override fun visitTypeOperator(expression: IrTypeOperatorCall): IrExpression {
            val result = super.visitTypeOperator(expression)
            if (result is IrTypeOperatorCall) {
                result.typeOperand = remapType(result.typeOperand)
                result.type = remapType(result.type)
            }
            return result
        }

        override fun visitCall(expression: IrCall): IrExpression {
            expression.transformChildrenVoid(this)

            val callee = expression.symbol.owner
            val parentClass = callee.parentClassOrNull ?: return expression
            val replacement = replacements[parentClass.symbol] ?: return expression

            val sharedClass = replacement.sharedClassInfo.irClass
            val sharedFunction = sharedClass.simpleFunctions()
                .firstOrNull { it.name == callee.name && it.origin == callee.origin }
                ?: return expression

            val builder = context.createIrBuilder(expression.symbol, expression.startOffset, expression.endOffset)
            return builder.irCall(sharedFunction).apply {
                for (i in expression.arguments.indices) {
                    if (i < arguments.size) {
                        arguments[i] = expression.arguments[i]
                    }
                }
            }
        }

        override fun visitConstructorCall(expression: IrConstructorCall): IrExpression {
            expression.transformChildrenVoid(this)

            val constructorClass = expression.symbol.owner.parentAsClass
            val replacement = replacements[constructorClass.symbol] ?: return expression

            val sharedInfo = replacement.sharedClassInfo
            val sharedConstructor = sharedInfo.irClass.primaryConstructor!!

            val builder = context.createIrBuilder(
                expression.symbol,
                expression.startOffset,
                expression.endOffset
            )

            val originalCtor = replacement.originalConstructor
            val regularParams = originalCtor.parameters.filter { it.kind == IrParameterKind.Regular }
            val capturedParamCount = regularParams.size - 1
            val firstRegularIdx = originalCtor.parameters.indexOf(regularParams[0])

            return builder.irCall(sharedConstructor).apply {
                arguments[0] = IrRawFunctionReferenceImpl(
                    startOffset = expression.startOffset,
                    endOffset = expression.endOffset,
                    type = sharedInfo.funcField.type,
                    symbol = replacement.bridgedFunction.symbol,
                )

                for (i in 0 until capturedParamCount) {
                    arguments[i + 1] = expression.arguments[firstRegularIdx + i]
                }
                for (i in capturedParamCount until sharedInfo.capturedFields.size) {
                    arguments[i + 1] = defaultValueForType(sharedInfo.capturedFields[i].type, builder)
                }

                val completionIdx = originalCtor.parameters.indexOf(regularParams.last())
                arguments[sharedInfo.capturedFields.size + 1] = expression.arguments[completionIdx]
                    ?: builder.irNull()
            }
        }
    }
}
