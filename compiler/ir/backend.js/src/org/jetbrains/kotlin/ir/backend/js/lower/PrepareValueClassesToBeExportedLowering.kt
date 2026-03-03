/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.backend.common.DeclarationTransformer
import org.jetbrains.kotlin.backend.common.ir.ValueRemapper
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.lower.irBlockBody
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.ir.JsIrBuilder
import org.jetbrains.kotlin.ir.backend.js.ir.isExported
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.builders.irSetField
import org.jetbrains.kotlin.ir.builders.irTemporary
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOriginImpl
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.declarations.isSingleFieldValueClass
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.irAttribute
import org.jetbrains.kotlin.ir.symbols.IrReturnTargetSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.copyFunctionSignatureAsStaticFrom
import org.jetbrains.kotlin.ir.util.erasedUpperBound
import org.jetbrains.kotlin.ir.util.getInlineClassBackingField
import org.jetbrains.kotlin.ir.util.primaryConstructor
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import kotlin.getValue

/**
 * In most cases, the exported value classes are represented as a boxed instance of the value class;
 * however, after stabilizing boxing/unboxing for external functions (KT-25943), to not break this behavior,
 * we need to box/unbox exported value classes in return/parameter position of external functions
 *
 * Unboxing logic is the same as for the regular value classes (just reading a field from a boxed value class);
 * however, to not replicate effects inside the initialization of the value class,
 * the boxing logic should differ from the regular value classes' boxing (there, it's just calling the class's primary constructor).
 *
 * For these purposes, we introduce a special top-level function to box the value class returned by an external declaration.
 *
 * The function just creates an object with the value class prototype and sets the value to its field:
 *
 * Before:
 * ```kotlin
 * @JsExport
 * value class Foo(val value: String) {
 *   init { println("Effect!") }
 * }
 *```
 *
 * After:
 * ```kotlin
 * fun Foo_box(value: String): Foo {
 *   val boxed = Object.create(Foo.prototype)
 *   boxed.value = value
 *   return boxed
 * }
 *
 * @JsExport
 * value class Foo(val value: String) {
 *   init { println("Effect!") }
 * }
 *```
 */
class PrepareValueClassesToBeExportedLowering(private val context: JsIrBackendContext) : DeclarationTransformer {
    private val jsObjectCreateSymbol = context.symbols.jsObjectCreateSymbol
    private val jsExportIgnoreAnnotation = context.symbols.jsExportIgnoreAnnotationSymbol.owner.constructors.single()

    companion object {
        private val EXPORTED_VALUE_CLASS_BOX_FUNCTION by IrDeclarationOriginImpl.Regular
    }

    override fun transformFlat(declaration: IrDeclaration): List<IrDeclaration>? {
        if (declaration !is IrClass || !declaration.isSingleFieldValueClass || !declaration.isExported(context)) {
            return null
        }

        val boxFunction = declaration.generateBoxFunction()
            .also(declaration::exportedValueClassBoxFunction::set)

        return listOf(boxFunction, declaration)
    }

    private fun IrClass.generateBoxFunction(): IrSimpleFunction {
        val primaryConstructor = this.primaryConstructor ?: error("Value class without primary constructor")
        val field = getInlineClassBackingField(this)
        return context.irFactory.buildFun {
            updateFrom(primaryConstructor)
            name = computeNameForBoxFunction()
            origin = EXPORTED_VALUE_CLASS_BOX_FUNCTION
            startOffset = UNDEFINED_OFFSET
            endOffset = UNDEFINED_OFFSET
        }.also { boxFunction ->
            boxFunction.parent = parent
            boxFunction.copyFunctionSignatureAsStaticFrom(primaryConstructor, typeParametersFromContext = typeParameters)
            boxFunction.annotations += JsIrBuilder.buildAnnotation(jsExportIgnoreAnnotation.symbol)
            boxFunction.body = context.createIrBuilder(boxFunction.symbol)
                .irBlockBody(boxFunction) {
                    val valueParameter = boxFunction.parameters.single { it.kind == IrParameterKind.Regular }
                    val boxedType = boxFunction.returnType
                    val boxed = irTemporary(
                        irCall(jsObjectCreateSymbol, boxedType, listOf(boxedType)),
                        "box_container"
                    )

                    +irSetField(irGet(boxed), field, irGet(valueParameter))
                    +irReturn(irGet(boxed))
                }
        }
    }

    private fun IrClass.computeNameForBoxFunction(): Name =
        Name.identifier("${name.asString()}-box")
}

var IrClass.exportedValueClassBoxFunction: IrSimpleFunction? by irAttribute(copyByDefault = true)

/**
 * For external declarations we need to generate boxing/unboxing code, so @JsExport value class continue
 * to keep unboxed on the JavaScript side (with `external`) and boxed after receiveing it from JS (via `external`)
 *
 * - We should unbox such a value class in the following cases:
 *     - Passing it as a parameter to an external function/constructor
 *     - Returning the value class from an overridden external method/property
 *
 * - We should box such a value class in the following cases:
 *     - Returned value from a function/property getter
 *     - If we're inside an overridden external method that accepts a value class as a parameter (the parameter should be boxed)
 */
class AutoboxingForExportedValueClassesForExternalsLowering(private val jsContext: JsIrBackendContext) : AbstractValueUsageLowering(jsContext) {
    private val boxIntrinsic = jsContext.symbols.jsBoxIntrinsic
    private val unboxIntrinsic = jsContext.symbols.jsUnboxIntrinsic

    override fun IrExpression.useExpressionAsType(actualType: IrType, expectedType: IrType): IrExpression =
        this

    override fun IrExpression.useAsValueArgument(expression: IrFunctionAccessExpression, parameter: IrValueParameter): IrExpression =
        whenExpectAnExportedValueClass(parameter.type) {
            if (expression.symbol.owner.isOriginallyExternal()) {
                autoboxCall(parameter.type, unboxIntrinsic)
            } else this
        }

    override fun IrExpression.useAsReturnValue(returnTarget: IrReturnTargetSymbol): IrExpression {
        val returnTarget = (returnTarget.owner as? IrSimpleFunction) ?: return this
        val originallyExternal = returnTarget.findOverriddenExternalFunction()
        if (originallyExternal == null || returnTarget.isInline) return this
        return whenExpectAnExportedValueClass(originallyExternal.returnType) {
            autoboxCall(originallyExternal.returnType, unboxIntrinsic)
        }
    }

    override fun visitCall(expression: IrCall): IrExpression {
        val call = super.visitCall(expression) as? IrCall ?: return expression
        val owner = call.symbol.owner
        if (!owner.isOriginallyExternal()) return call
        return call.whenExpectAnExportedValueClass(call.type) {
            call.autoboxCall(call.type, boxIntrinsic)
        }
    }

    override fun visitSimpleFunction(declaration: IrSimpleFunction): IrStatement {
        val functionBody = declaration.body

        if (!declaration.isExternal && functionBody != null) {
            val originalExternal = declaration.findOverriddenExternalFunction()

            if (originalExternal != null) {
                val valueClassParameters = originalExternal.parameters.mapIndexedNotNull { index, originalParameter ->
                    runIf(isExportedValueClass(originalParameter.type)) { declaration.parameters[index] }
                }

                val container = functionBody as IrBlockBody

                declaration.body = context.createIrBuilder(declaration.symbol).irBlockBody {
                    val parametersToTmp = valueClassParameters.associateBy(keySelector = { it.symbol }) {
                        irTemporary(
                            irGet(it).autoboxCall(it.type, boxIntrinsic),
                            "box"
                        ).symbol
                    }

                    container.transformChildrenVoid(ValueRemapper(parametersToTmp))
                    +container.statements
                }
            }
        }

        return super.visitSimpleFunction(declaration)
    }

    private fun IrFunction.isOriginallyExternal(): Boolean =
        when (this) {
            is IrConstructor -> isExternal
            is IrSimpleFunction -> findOverriddenExternalFunction() != null
        }

    private fun IrSimpleFunction.findOverriddenExternalFunction(): IrSimpleFunction? =
        when {
            isExternal -> this
            else -> overriddenSymbols.firstNotNullOfOrNull { it.owner.findOverriddenExternalFunction() }
        }

    private fun IrExpression.autoboxCall(valueClassType: IrType, autoboxIntrinsic: IrSimpleFunctionSymbol) =
        JsIrBuilder.buildCall(autoboxIntrinsic, valueClassType, typeArguments = listOf(valueClassType, valueClassType))
            .also { it.arguments[0] = this }

    private inline fun IrExpression.whenExpectAnExportedValueClass(expectedType: IrType, process: () -> IrExpression): IrExpression =
        when {
            isExportedValueClass(expectedType) -> process()
            else -> this
        }

    private fun isExportedValueClass(type: IrType) =
        type is IrSimpleType && type.erasedUpperBound.let { it.isSingleFieldValueClass && it.isExported(jsContext) }
}