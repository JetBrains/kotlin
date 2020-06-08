/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.backend.common.lower

import org.jetbrains.kotlin.backend.common.BackendContext
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.ValueParameterDescriptorImpl
import org.jetbrains.kotlin.ir.DescriptorBasedIr
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classifierOrFail
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.name.Name

class IrLoweringContext(backendContext: BackendContext) : IrGeneratorContextBase(backendContext.irBuiltIns)

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
}

open class VariableRemapper(val mapping: Map<IrValueParameter, IrValueDeclaration>) : AbstractVariableRemapper() {
    override fun remapVariable(value: IrValueDeclaration): IrValueDeclaration? =
        mapping[value]
}

@DescriptorBasedIr
class VariableRemapperDesc(val mapping: Map<ValueDescriptor, IrValueParameter>) : AbstractVariableRemapper() {
    override fun remapVariable(value: IrValueDeclaration): IrValueDeclaration? =
        mapping[value.descriptor]
}

fun BackendContext.createIrBuilder(
    symbol: IrSymbol,
    startOffset: Int = UNDEFINED_OFFSET,
    endOffset: Int = UNDEFINED_OFFSET
) =
    DeclarationIrBuilder(IrLoweringContext(this), symbol, startOffset, endOffset)


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

fun IrBuilderWithScope.irCatch(catchParameter: IrVariable) =
    IrCatchImpl(
        startOffset, endOffset,
        catchParameter
    )

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
}

@OptIn(DescriptorBasedIr::class)
fun IrConstructor.callsSuper(irBuiltIns: IrBuiltIns): Boolean {
    val constructedClass = parent as IrClass
    val superClass = constructedClass.superTypes
        .mapNotNull { it as? IrSimpleType }
        .firstOrNull { (it.classifier.owner as IrClass).run { kind == ClassKind.CLASS || kind == ClassKind.ANNOTATION_CLASS || kind == ClassKind.ENUM_CLASS } }
        ?: irBuiltIns.anyType
    var callsSuper = false
    var numberOfCalls = 0
    acceptChildrenVoid(object : IrElementVisitorVoid {
        override fun visitElement(element: IrElement) {
            element.acceptChildrenVoid(this)
        }

        override fun visitClass(declaration: IrClass) {
            // Skip nested
        }

        override fun visitDelegatingConstructorCall(expression: IrDelegatingConstructorCall) {
            assert(++numberOfCalls == 1) { "More than one delegating constructor call: ${symbol.owner}" }
            val delegatingClass = expression.symbol.owner.parent as IrClass
            // TODO: figure out why Lazy IR multiplies Declarations for descriptors and fix it
            // It happens because of IrBuiltIns whose IrDeclarations are different for runtime and test
            if (delegatingClass.descriptor == superClass.classifierOrFail.descriptor)
                callsSuper = true
            else if (delegatingClass.descriptor != constructedClass.descriptor)
                throw AssertionError(
                    "Expected either call to another constructor of the class being constructed or" +
                            " call to super class constructor. But was: $delegatingClass with '${delegatingClass.name}' name"
                )
        }
    })
    assert(numberOfCalls == 1) { "Expected exactly one delegating constructor call but none encountered: ${symbol.owner}" }
    return callsSuper
}

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
