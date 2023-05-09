/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower

import org.jetbrains.kotlin.backend.common.BackendContext
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.ValueParameterDescriptorImpl
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.linkage.partial.isPartialLinkageRuntimeError
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.symbols.IrValueParameterSymbol
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classifierOrFail
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.name.Name

class DeclarationIrBuilder(
    generatorContext: IrGeneratorContext,
    symbol: IrSymbol,
    startOffset: Int = UNDEFINED_OFFSET, endOffset: Int = UNDEFINED_OFFSET
) : IrBuilderWithScope(
    generatorContext,
    Scope(symbol),
    startOffset,
    endOffset
)

abstract class AbstractVariableRemapper : IrElementTransformerVoid() {
    protected abstract fun remapVariable(value: IrValueDeclaration): IrValueDeclaration?

    override fun visitGetValue(expression: IrGetValue): IrExpression =
        remapVariable(expression.symbol.owner)?.let {
            IrGetValueImpl(expression.startOffset, expression.endOffset, it.type, it.symbol, expression.origin)
        } ?: expression

    override fun visitSetValue(expression: IrSetValue): IrExpression {
        expression.transformChildrenVoid()
        return remapVariable(expression.symbol.owner)?.let {
            IrSetValueImpl(expression.startOffset, expression.endOffset, expression.type, it.symbol, expression.value, expression.origin)
        } ?: expression
    }
}

open class VariableRemapper(val mapping: Map<IrValueParameter, IrValueDeclaration>) : AbstractVariableRemapper() {
    override fun remapVariable(value: IrValueDeclaration): IrValueDeclaration? =
        mapping[value]
}

fun IrBuiltIns.createIrBuilder(
    symbol: IrSymbol,
    startOffset: Int = UNDEFINED_OFFSET,
    endOffset: Int = UNDEFINED_OFFSET
) =
    DeclarationIrBuilder(IrGeneratorContextBase(this), symbol, startOffset, endOffset)

fun BackendContext.createIrBuilder(
    symbol: IrSymbol,
    startOffset: Int = UNDEFINED_OFFSET,
    endOffset: Int = UNDEFINED_OFFSET
) =
    irBuiltIns.createIrBuilder(symbol, startOffset, endOffset)


fun <T : IrBuilder> T.at(element: IrElement) = this.at(element.startOffset, element.endOffset)

/**
 * Builds [IrBlock] to be used instead of given expression.
 */
inline fun IrGeneratorWithScope.irBlock(
    expression: IrExpression, origin: IrStatementOrigin? = null,
    resultType: IrType? = expression.type,
    body: IrBlockBuilder.() -> Unit
) =
    this.irBlock(expression.startOffset, expression.endOffset, origin, resultType, body)

inline fun IrGeneratorWithScope.irComposite(
    expression: IrExpression, origin: IrStatementOrigin? = null,
    resultType: IrType? = expression.type,
    body: IrBlockBuilder.() -> Unit
) =
    this.irComposite(expression.startOffset, expression.endOffset, origin, resultType, body)

inline fun IrGeneratorWithScope.irBlockBody(irElement: IrElement, body: IrBlockBodyBuilder.() -> Unit) =
    this.irBlockBody(irElement.startOffset, irElement.endOffset, body)

fun IrBuilderWithScope.irIfThen(condition: IrExpression, thenPart: IrExpression) =
    IrIfThenElseImpl(startOffset, endOffset, context.irBuiltIns.unitType).apply {
        branches += IrBranchImpl(condition, thenPart)
    }

fun IrBuilderWithScope.irNot(arg: IrExpression) =
    primitiveOp1(startOffset, endOffset, context.irBuiltIns.booleanNotSymbol, context.irBuiltIns.booleanType, IrStatementOrigin.EXCL, arg)

fun IrBuilderWithScope.irThrow(arg: IrExpression) =
    IrThrowImpl(startOffset, endOffset, context.irBuiltIns.nothingType, arg)

fun IrBuilderWithScope.irCatch(catchParameter: IrVariable, result: IrExpression): IrCatch =
    IrCatchImpl(startOffset, endOffset, catchParameter, result)

fun IrBuilderWithScope.irImplicitCoercionToUnit(arg: IrExpression) =
    IrTypeOperatorCallImpl(
        startOffset, endOffset, context.irBuiltIns.unitType,
        IrTypeOperator.IMPLICIT_COERCION_TO_UNIT, context.irBuiltIns.unitType,
        arg
    )

open class IrBuildingTransformer(private val context: BackendContext) : IrElementTransformerVoid() {
    private var currentBuilder: IrBuilderWithScope? = null

    protected val builder: IrBuilderWithScope
        get() = currentBuilder!!

    private inline fun <T> withBuilder(symbol: IrSymbol, block: () -> T): T {
        val oldBuilder = currentBuilder
        currentBuilder = context.createIrBuilder(symbol)
        return try {
            block()
        } finally {
            currentBuilder = oldBuilder
        }
    }

    override fun visitFunction(declaration: IrFunction): IrStatement {
        withBuilder(declaration.symbol) {
            return super.visitFunction(declaration)
        }
    }

    override fun visitField(declaration: IrField): IrStatement {
        withBuilder(declaration.symbol) {
            // Transforms initializer:
            return super.visitField(declaration)
        }
    }

    override fun visitAnonymousInitializer(declaration: IrAnonymousInitializer): IrStatement {
        withBuilder(declaration.symbol) {
            return super.visitAnonymousInitializer(declaration)
        }
    }

    override fun visitEnumEntry(declaration: IrEnumEntry): IrStatement {
        withBuilder(declaration.symbol) {
            return super.visitEnumEntry(declaration)
        }
    }

    override fun visitScript(declaration: IrScript): IrStatement {
        withBuilder(declaration.symbol) {
            return super.visitScript(declaration)
        }
    }
}

enum class ConstructorDelegationKind {
    /** Calls another constructor of the same class. */
    CALLS_THIS,

    /** Calls the constructor of the super class. */
    CALLS_SUPER,

    /** The actual delegation is not known. The constructor call was replaced by a partial linkage error. */
    PARTIAL_LINKAGE_ERROR
}

fun IrConstructor.delegationKind(irBuiltIns: IrBuiltIns): ConstructorDelegationKind {
    val constructedClass = parent as IrClass
    val superClass = constructedClass.superTypes
        .mapNotNull { it as? IrSimpleType }
        .firstOrNull { (it.classifier.owner as IrClass).run { kind == ClassKind.CLASS || kind == ClassKind.ANNOTATION_CLASS || kind == ClassKind.ENUM_CLASS } }
        ?: irBuiltIns.anyType
    var callsSuper = false
    var numberOfDelegatingCalls = 0
    var hasPartialLinkageError = false
    acceptChildrenVoid(object : IrElementVisitorVoid {
        override fun visitElement(element: IrElement) {
            element.acceptChildrenVoid(this)
        }

        override fun visitClass(declaration: IrClass) {
            // Skip nested
        }

        override fun visitDelegatingConstructorCall(expression: IrDelegatingConstructorCall) {
            numberOfDelegatingCalls++
            val delegatingClass = expression.symbol.owner.parent as IrClass
            // TODO: figure out why Lazy IR multiplies Declarations for descriptors and fix it
            // It happens because of IrBuiltIns whose IrDeclarations are different for runtime and test
            if (delegatingClass.symbol == superClass.classifierOrFail)
                callsSuper = true
            else if (delegatingClass.symbol != constructedClass.symbol)
                throw AssertionError(
                    "Expected either call to another constructor of the class being constructed or" +
                            " call to super class constructor. But was: $delegatingClass with '${delegatingClass.name}' name"
                )
        }

        override fun visitExpression(expression: IrExpression) {
            hasPartialLinkageError = hasPartialLinkageError || expression.isPartialLinkageRuntimeError()
            super.visitExpression(expression)
        }
    })

    val delegationKind: ConstructorDelegationKind? = when (numberOfDelegatingCalls) {
        0 -> if (hasPartialLinkageError) ConstructorDelegationKind.PARTIAL_LINKAGE_ERROR else null
        1 -> if (callsSuper) ConstructorDelegationKind.CALLS_SUPER else ConstructorDelegationKind.CALLS_THIS
        else -> null
    }

    if (delegationKind != null)
        return delegationKind
    else
        throw AssertionError("Expected exactly one delegating constructor call but $numberOfDelegatingCalls encountered: ${symbol.owner}")
}

@Deprecated(
    "Replaced by delegationKind() that is aware of the possible partial linkage side effects",
    ReplaceWith("delegationKind(irBuiltIns)")
)
fun IrConstructor.callsSuper(irBuiltIns: IrBuiltIns): Boolean = delegationKind(irBuiltIns) == ConstructorDelegationKind.CALLS_SUPER

fun ParameterDescriptor.copyAsValueParameter(newOwner: CallableDescriptor, index: Int, name: Name = this.name) = when (this) {
    is ValueParameterDescriptor -> this.copy(newOwner, name, index)
    is ReceiverParameterDescriptor -> ValueParameterDescriptorImpl(
        containingDeclaration = newOwner,
        original = null,
        index = index,
        annotations = annotations,
        name = name,
        outType = type,
        declaresDefaultValue = false,
        isCrossinline = false,
        isNoinline = false,
        varargElementType = null,
        source = source
    )
    else -> throw Error("Unexpected parameter descriptor: $this")
}

fun IrGetValue.actualize(classActualizer: (IrClass) -> IrClass, functionActualizer: (IrFunction) -> IrFunction): IrGetValue {
    val symbol = symbol
    if (symbol !is IrValueParameterSymbol) {
        return this
    }

    val parameter = symbol.owner
    val newSymbol = when (val parent = parameter.parent) {
        is IrClass -> {
            assert(parameter == parent.thisReceiver)
            classActualizer(parent).thisReceiver!!
        }

        is IrFunction -> {
            val actualizedFunction = functionActualizer(parent)
            when (parameter) {
                parent.dispatchReceiverParameter -> actualizedFunction.dispatchReceiverParameter!!
                parent.extensionReceiverParameter -> actualizedFunction.extensionReceiverParameter!!
                else -> {
                    assert(parent.valueParameters[parameter.index] == parameter)
                    actualizedFunction.valueParameters[parameter.index]
                }
            }
        }

        else -> error(parent)
    }

    return IrGetValueImpl(startOffset, endOffset, newSymbol.type, newSymbol.symbol, origin)
}