/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrInstanceInitializerCallImpl
import org.jetbrains.kotlin.ir.irAttribute
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrTransformer
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.utils.addToStdlib.applyIf
import org.jetbrains.kotlin.utils.addToStdlib.shouldNotBeCalled
import org.jetbrains.kotlin.utils.memoryOptimizedPlus


/**
 * This attribute is used to store declarations as they were at this lowering.
 * This is needed to avoid creating fake overrides of declarations at later state.
 * @see org.jetbrains.kotlin.ir.overrides.IrFakeOverrideBuilder.buildFakeOverridesForClassUsingOverriddenSymbols for more details.
 */
var IrClass.declarationsAtFunctionReferenceLowering: List<IrDeclaration>? by irAttribute(copyByDefault = true)

/**
 * This lowering transforms [IrRichFunctionReference] nodes to an anonymous class.
 *
 * The class would have:
 *   * Constructor capturing all values from [IrRichFunctionReference.boundValues], and storing them to fields
 *   * A method overriding [IrRichFunctionReference.overriddenFunctionSymbol], with body moved from [IrRichFunctionReference.invokeFunction]
 *   * [IrRichFunctionReference.type] as a super-interface type (typically `[K][Suspend]FunctionN`,
 *     or fun interface the reference was sam converted to)
 *
 * Platforms can customize:
 *   * Super-class with platform-specific reference implementation details by overriding [getSuperClassType] method
 *   * Equality/hashCode/toString and other reflection methods implementation by adding methods in overridden [generateExtraMethods] method
 *   * exact names/origins of generated classes/methods by overriding corresponding methods
 *
 * For example, the following code:
 * ```kotlin
 * fun foo1(l: () -> String): String {
 *   return l()
 * }
 * fun <FooTP> foo2(v: FooTP, l: (FooTP) -> String): String {
 *   return l(v)
 * }
 *
 * private fun <T> bar(t: T): String { /* ... */ }
 *
 * fun <BarTP> bar(v: BarTP): String {
 *   return foo1(v::bar/*<T=BarTP>*/) + foo2(v) { bar/*<T=BarTP>*/(it) }
 * }
 * ```
 *
 * is lowered into:
 * ```kotlin
 * fun <BarTP> bar(v: BarTP): String {
 *   /*local*/ class <local-platform-specific-name-1>(p$0: BarTP) : KFunction0<String>, PlatformSpecificSuperType() {
 *     private val f$0: BarTP = p$0
 *     override fun invoke() = bar<BarTP>(f$0)
 *     // some platform specific reflection information
 *   }
 *   /*local*/ class <local-platform-specific-name-2> : Function1<BarTP, String>, PlatformSpecificSuperTypeProbablyAny() {
 *     override fun invoke(p0: BarTP) = bar<BarTP>(p0)
 *   }
 *   return foo1(<local-platform-specific-name-1>(v)) + foo2(v, <local-platform-specific-name-2>())
 * }
 * ```
 *
 * Note that as all these classes are defined as local ones, they don't need to explicitly capture local variables or any type parameters.
 * But it can happen, that [LocalDeclarationsLowering] would later capture something additional into the classes.
 */
abstract class AbstractFunctionReferenceLowering<C : CommonBackendContext>(val context: C) : FileLoweringPass {
    override fun lower(irFile: IrFile) {
        irFile.transform(object : IrTransformer<IrDeclaration?>() {
            override fun visitClass(declaration: IrClass, data: IrDeclaration?): IrStatement {
                if (declaration.isFun || declaration.symbol.isSuspendFunction() || declaration.symbol.isKSuspendFunction()) {
                    declaration.declarationsAtFunctionReferenceLowering = declaration.declarations.toList()
                }
                declaration.transformChildren(this, declaration)
                return declaration
            }

            override fun visitBody(body: IrBody, data: IrDeclaration?): IrBody {
                return data!!.factory.stageController.restrictTo(data) {
                    super.visitBody(body, data)
                }
            }

            override fun visitDeclaration(declaration: IrDeclarationBase, data: IrDeclaration?): IrStatement {
                declaration.transformChildren(this, declaration)
                return declaration
            }

            override fun visitRichFunctionReference(expression: IrRichFunctionReference, data: IrDeclaration?): IrExpression {
                expression.transformChildren(this, data)
                val irBuilder = context.createIrBuilder(
                    data!!.symbol,
                    expression.startOffset, expression.endOffset
                )

                val clazz = buildClass(expression, irBuilder.scope.getLocalDeclarationParent())
                val constructor = clazz.primaryConstructor!!
                val newExpression = irBuilder.irCallConstructor(constructor.symbol, emptyList()).apply {
                    origin = getConstructorCallOrigin(expression)
                    for ((index, value) in expression.boundValues.withIndex()) {
                        arguments[index] = value
                    }
                    for (index in expression.boundValues.size until arguments.size) {
                        arguments[index] = irBuilder.getExtraConstructorArgument(constructor.parameters[index], expression)
                    }
                }
                return irBuilder.irBlock {
                    +clazz
                    +newExpression
                }
            }

            override fun visitFunctionReference(expression: IrFunctionReference, data: IrDeclaration?): IrExpression {
                shouldNotBeCalled()
            }
        }, null)
    }

    private fun buildClass(functionReference: IrRichFunctionReference, parent: IrDeclarationParent): IrClass {
        val functionReferenceClass = context.irFactory.buildClass {
            startOffset = functionReference.startOffset
            endOffset = functionReference.endOffset
            origin = getClassOrigin(functionReference)
            name = getReferenceClassName(functionReference)
            visibility = DescriptorVisibilities.LOCAL
        }.apply {
            this.parent = parent
            createThisReceiverParameter()
        }
        val superClassType = getSuperClassType(functionReference)
        val superInterfaceType = functionReference.type.removeProjections()
        functionReferenceClass.superTypes =
            listOf(superClassType, superInterfaceType) memoryOptimizedPlus getAdditionalInterfaces(functionReference)
        val constructor = functionReferenceClass.addConstructor {
            origin = getConstructorOrigin(functionReference)
            isPrimary = true
        }.apply {
            parameters = functionReference.boundValues.mapIndexed { index, value ->
                buildValueParameter(this) {
                    name = Name.identifier("p${index}")
                    startOffset = value.startOffset
                    endOffset = value.endOffset
                    type = value.type
                    kind = IrParameterKind.Regular
                }
            } + getExtraConstructorParameters(this, functionReference)
            body = context.createIrBuilder(symbol, this.startOffset, this.endOffset).irBlockBody {
                +generateSuperClassConstructorCall(this@apply, superClassType, functionReference)
                +IrInstanceInitializerCallImpl(this.startOffset, this.endOffset, functionReferenceClass.symbol, context.irBuiltIns.unitType)
            }
        }

        val fields = functionReference.boundValues.mapIndexed { index, captured ->
            functionReferenceClass.addField {
                startOffset = captured.startOffset
                endOffset = captured.endOffset
                name = Name.identifier("f\$${index}")
                visibility = DescriptorVisibilities.PRIVATE
                isFinal = true
                type = captured.type
            }.apply {
                val builder = context.createIrBuilder(symbol, startOffset, endOffset)
                initializer = builder.irExprBody(builder.irGet(constructor.parameters[index]))
            }
        }
        buildInvokeMethod(
            functionReference,
            functionReferenceClass,
            superInterfaceType,
            fields,
        ).apply {
            postprocessInvoke(this, functionReference)
        }

        generateExtraMethods(functionReferenceClass, functionReference)

        val superInterfaceClass = superInterfaceType.classOrFail.owner

        functionReferenceClass.addFakeOverrides(
            context.typeSystem,
            buildMap {
                superInterfaceClass.declarationsAtFunctionReferenceLowering?.let { put(superInterfaceClass, it) }
            }
        )
        postprocessClass(functionReferenceClass, functionReference)
        return functionReferenceClass
    }

    /**
     * This function is very similar to [org.jetbrains.kotlin.backend.jvm.lower.FunctionReferenceLowering.FunctionReferenceBuilder.createInvokeMethod].
     * If you make any changes, don't forget to also change the other one.
     */
    private fun buildInvokeMethod(
        functionReference: IrRichFunctionReference,
        functionReferenceClass: IrClass,
        superInterfaceType: IrType,
        boundFields: List<IrField>
    ): IrSimpleFunction {
        val superFunction = functionReference.overriddenFunctionSymbol.owner
        val invokeFunction = functionReference.invokeFunction
        val isLambda = functionReference.origin.isLambda
        return functionReferenceClass.addFunction {
            setSourceRange(if (isLambda) invokeFunction else functionReference)
            origin = getInvokeMethodOrigin(functionReference)
            name = superFunction.name
            returnType = invokeFunction.returnType
            isOperator = superFunction.isOperator
            isSuspend = superFunction.isSuspend
        }.apply {
            attributeOwnerId = functionReference.attributeOwnerId
            annotations = invokeFunction.annotations

            parameters += createDispatchReceiverParameterWithClassParent()
            require(superFunction.typeParameters.isEmpty()) { "Fun interface abstract function can't have type parameters" }

            val typeSubstitutor = IrTypeSubstitutor(
                extractTypeParameters(superInterfaceType.classOrFail.owner).map { it.symbol },
                (superInterfaceType as IrSimpleType).arguments,
                allowEmptySubstitution = true
            )

            val nonDispatchParameters = superFunction.nonDispatchParameters.mapIndexed { i, superParameter ->
                val oldParameter = invokeFunction.parameters[i + boundFields.size]
                superParameter.copyTo(
                    this,
                    startOffset = if (isLambda) oldParameter.startOffset else UNDEFINED_OFFSET,
                    endOffset = if (isLambda) oldParameter.endOffset else UNDEFINED_OFFSET,
                    name = oldParameter.name,
                    type = typeSubstitutor.substitute(superParameter.type),
                    defaultValue = null,
                ).apply { copyAnnotationsFrom(oldParameter) }
            }
            this.parameters += nonDispatchParameters
            val overriddenMethodOfAny = superFunction.findOverriddenMethodOfAny()
            overriddenSymbols = if (overriddenMethodOfAny == null)
                listOf(superFunction.symbol)
            else functionReferenceClass.superTypes.mapNotNull { superType ->
                superType.classOrFail.owner.functions.firstOrNull { it.overrides(overriddenMethodOfAny) }?.symbol
            }

            val builder = context.createIrBuilder(symbol).applyIf(isLambda) { at(invokeFunction.body!!) }
            body = builder.irBlockBody {
                val variablesMapping = buildMap {
                    for ((index, field) in boundFields.withIndex()) {
                        put(invokeFunction.parameters[index], irTemporary(irGetField(irGet(dispatchReceiverParameter!!), field)))
                    }
                    for ((index, parameter) in nonDispatchParameters.withIndex()) {
                        val invokeParameter = invokeFunction.parameters[index + boundFields.size]
                        if (parameter.type != invokeParameter.type) {
                            put(invokeParameter, irTemporary(irGet(parameter).implicitCastTo(invokeParameter.type)))
                        } else {
                            put(invokeParameter, parameter)
                        }
                    }
                }
                val transformedBody = invokeFunction.body!!.transform(object : VariableRemapper(variablesMapping) {
                    override fun visitReturn(expression: IrReturn): IrExpression {
                        if (expression.returnTargetSymbol == invokeFunction.symbol) {
                            expression.returnTargetSymbol = this@apply.symbol
                        }
                        return super.visitReturn(expression)
                    }

                    override fun visitDeclaration(declaration: IrDeclarationBase): IrStatement {
                        if (declaration.parent == invokeFunction)
                            declaration.parent = this@apply
                        return super.visitDeclaration(declaration)
                    }
                }, null)
                when (transformedBody) {
                    is IrBlockBody -> +transformedBody.statements
                    is IrExpressionBody -> +irReturn(transformedBody.expression)
                    else -> error("Unexpected body type: ${transformedBody::class.simpleName}")
                }
            }
        }
    }

    protected open fun postprocessClass(functionReferenceClass: IrClass, functionReference: IrRichFunctionReference) {}
    protected open fun postprocessInvoke(invokeFunction: IrSimpleFunction, functionReference: IrRichFunctionReference) {}
    protected open fun generateExtraMethods(functionReferenceClass: IrClass, reference: IrRichFunctionReference) {}

    protected open fun getExtraConstructorParameters(
        constructor: IrConstructor,
        reference: IrRichFunctionReference,
    ): List<IrValueParameter> = emptyList()

    protected open fun IrBuilderWithScope.getExtraConstructorArgument(
        parameter: IrValueParameter,
        reference: IrRichFunctionReference,
    ): IrExpression? = null

    protected abstract fun IrBuilderWithScope.generateSuperClassConstructorCall(
        constructor: IrConstructor,
        superClassType: IrType,
        functionReference: IrRichFunctionReference,
    ): IrDelegatingConstructorCall

    protected abstract fun getReferenceClassName(reference: IrRichFunctionReference): Name
    protected abstract fun getSuperClassType(reference: IrRichFunctionReference): IrType
    protected open fun getAdditionalInterfaces(reference: IrRichFunctionReference): List<IrType> = emptyList()
    protected abstract fun getClassOrigin(reference: IrRichFunctionReference): IrDeclarationOrigin
    protected abstract fun getConstructorOrigin(reference: IrRichFunctionReference): IrDeclarationOrigin
    protected abstract fun getInvokeMethodOrigin(reference: IrRichFunctionReference): IrDeclarationOrigin
    protected abstract fun getConstructorCallOrigin(reference: IrRichFunctionReference): IrStatementOrigin?
}
