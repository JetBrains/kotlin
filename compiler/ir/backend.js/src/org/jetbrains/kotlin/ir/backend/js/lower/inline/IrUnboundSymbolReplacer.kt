/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower.inline

import org.jetbrains.kotlin.backend.common.pop
import org.jetbrains.kotlin.backend.common.push
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.IrReturnTargetSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.types.classifierOrFail
import org.jetbrains.kotlin.ir.types.toIrType
import org.jetbrains.kotlin.ir.util.ExternalDependenciesGenerator
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.ir.util.referenceClassifier
import org.jetbrains.kotlin.ir.util.referenceFunction
import org.jetbrains.kotlin.ir.visitors.*

// backend.native/compiler/ir/backend.native/src/org/jetbrains/kotlin/ir/util/IrUnboundSymbolReplacer.kt
@Deprecated("")
internal fun IrModuleFragment.replaceUnboundSymbols(context: JsIrBackendContext) {
    val collector = DeclarationSymbolCollector()
    with(collector) {
        with(irBuiltins) {
            for (op in arrayOf(eqeqeqFun, eqeqFun, throwNpeFun, booleanNotFun, noWhenBranchMatchedExceptionFun) +
                    lessFunByOperandType.values +
                    lessOrEqualFunByOperandType.values +
                    greaterOrEqualFunByOperandType.values +
                    greaterFunByOperandType.values +
                    ieee754equalsFunByOperandType.values) {
                register(op.symbol)
            }
        }
    }
    this.acceptVoid(collector)

    val symbolTable = context.symbolTable

    this.transformChildrenVoid(
        IrUnboundSymbolReplacer(
            symbolTable,
            collector.descriptorToSymbol
        )
    )

    // Generate missing external stubs:
    // TODO: ModuleGenerator::generateUnboundSymbolsAsDependencies(IRModuleFragment) is private function :/
    @Suppress("DEPRECATION")
    ExternalDependenciesGenerator(
        descriptor,
        symbolTable = context.symbolTable,
        irBuiltIns = context.irBuiltIns
    ).generateUnboundSymbolsAsDependencies(this)
}

private class DeclarationSymbolCollector : IrElementVisitorVoid {

    val descriptorToSymbol = mutableMapOf<DeclarationDescriptor, IrSymbol>()

    fun register(symbol: IrSymbol) {
        descriptorToSymbol[symbol.descriptor] = symbol
    }

    override fun visitElement(element: IrElement) {
        element.acceptChildrenVoid(this)

        if (element is IrSymbolOwner && element !is IrAnonymousInitializer) {
            register(element.symbol)
        }
    }
}

private class IrUnboundSymbolReplacer(
    val symbolTable: SymbolTable,
    val descriptorToSymbol: Map<DeclarationDescriptor, IrSymbol>
) : IrElementTransformerVoid() {

    private val localDescriptorToSymbol = mutableMapOf<DeclarationDescriptor, MutableList<IrSymbol>>()

    private inline fun <R> withLocal(symbol: IrSymbol?, block: () -> R): R {
        if (symbol == null) return block()

        val locals = localDescriptorToSymbol.getOrPut(symbol.descriptor) { mutableListOf() }
        locals.add(symbol)
        return try {
            block()
        } finally {
            locals.removeAt(locals.lastIndex)
        }
    }

    private inline fun <reified D : DeclarationDescriptor, reified S : IrSymbol> S.replace(
        referenceSymbol: (SymbolTable, D) -> S): S? {

        if (this.isBound) {
            return null
        }

        localDescriptorToSymbol[this.descriptor]?.lastOrNull()?.let {
            return it as S
        }


        descriptorToSymbol[this.descriptor]?.let {
            return it as S
        }

        return referenceSymbol(symbolTable, this.descriptor as D)
    }

    private inline fun <reified D : DeclarationDescriptor, reified S : IrSymbol> S.replaceOrSame(
        referenceSymbol: (SymbolTable, D) -> S): S = this.replace(referenceSymbol) ?: this

    private inline fun <reified S : IrSymbol> S.replaceLocal(): S? {
        return if (this.isBound) {
            null
        } else {
            (localDescriptorToSymbol[this.descriptor]?.lastOrNull() ?: descriptorToSymbol[this.descriptor]) as S
        }
    }

    override fun visitGetValue(expression: IrGetValue): IrExpression {
        val symbol = expression.symbol.replaceLocal() ?: return super.visitGetValue(expression)

        expression.transformChildrenVoid(this)
        return with(expression) {
            IrGetValueImpl(startOffset, endOffset, expression.type, symbol, origin)
        }
    }

    override fun visitSetVariable(expression: IrSetVariable): IrExpression {
        val symbol = expression.symbol.replaceLocal() ?: return super.visitSetVariable(expression)

        expression.transformChildrenVoid(this)
        return with(expression) {
            IrSetVariableImpl(startOffset, endOffset, expression.type, symbol, value, origin)
        }
    }

    override fun visitGetObjectValue(expression: IrGetObjectValue): IrExpression {
        val symbol = expression.symbol.replace(SymbolTable::referenceClass) ?:
                return super.visitGetObjectValue(expression)

        expression.transformChildrenVoid(this)
        return with(expression) {
            IrGetObjectValueImpl(startOffset, endOffset, type, symbol)
        }
    }

    override fun visitGetEnumValue(expression: IrGetEnumValue): IrExpression {
        val symbol = expression.symbol.replace(SymbolTable::referenceEnumEntry) ?:
                return super.visitGetEnumValue(expression)

        expression.transformChildrenVoid(this)
        return with(expression) {
            IrGetEnumValueImpl(startOffset, endOffset, type, symbol)
        }
    }

    override fun visitClassReference(expression: IrClassReference): IrExpression {
        val symbol = expression.symbol.replace(SymbolTable::referenceClassifier)
                ?: return super.visitClassReference(expression)

        expression.transformChildrenVoid(this)
        return with(expression) {
            IrClassReferenceImpl(startOffset, endOffset, type, symbol, symbol.descriptor.toIrType(symbolTable = symbolTable))
        }
    }

    override fun visitClass(declaration: IrClass): IrStatement {
        withLocal(declaration.thisReceiver?.symbol) {
            return super.visitClass(declaration)
        }
    }

    override fun visitGetField(expression: IrGetField): IrExpression {
        val symbol = expression.symbol.replaceOrSame(SymbolTable::referenceField)

        val superQualifierSymbol = expression.superQualifierSymbol?.replaceOrSame(SymbolTable::referenceClass)

        if (symbol == expression.symbol && superQualifierSymbol == expression.superQualifierSymbol) {
            return super.visitGetField(expression)
        }

        expression.transformChildrenVoid(this)
        return with(expression) {
            IrGetFieldImpl(startOffset, endOffset, symbol, type, receiver, origin, superQualifierSymbol)
        }
    }

    override fun visitSetField(expression: IrSetField): IrExpression {
        val symbol = expression.symbol.replaceOrSame(SymbolTable::referenceField)

        val superQualifierSymbol = expression.superQualifierSymbol?.replaceOrSame(SymbolTable::referenceClass)

        if (symbol == expression.symbol && superQualifierSymbol == expression.superQualifierSymbol) {
            return super.visitSetField(expression)
        }

        expression.transformChildrenVoid(this)
        return with(expression) {
            IrSetFieldImpl(startOffset, endOffset, symbol, receiver, value, type /*wrong*/, origin, superQualifierSymbol)
        }
    }

    override fun visitCall(expression: IrCall): IrExpression {
        expression.replaceTypeArguments()

        val symbol = expression.symbol.replace(SymbolTable::referenceFunction) ?: expression.symbol

        val superQualifierSymbol = expression.superQualifierSymbol?.replaceOrSame(SymbolTable::referenceClass)

        if (symbol == expression.symbol && superQualifierSymbol == expression.superQualifierSymbol) {
            return super.visitCall(expression)
        }

        expression.transformChildrenVoid()
        return with(expression) {
            IrCallImpl(startOffset, endOffset, type, symbol, descriptor,
                    typeArgumentsCount,
                    origin, superQualifierSymbol).also {

                it.copyArgumentsFrom(this)
                it.copyTypeArgumentsFrom(this)
            }
        }
    }

    private fun IrMemberAccessExpression.replaceTypeArguments() {
        repeat(typeArgumentsCount) {
            putTypeArgument(it, getTypeArgument(it)/*?.toKotlinType()?.let { context.ir.translateErased(it) }*/)
        }
    }

    private fun IrMemberAccessExpressionBase.copyArgumentsFrom(original: IrMemberAccessExpression) {
        dispatchReceiver = original.dispatchReceiver
        extensionReceiver = original.extensionReceiver
        original.descriptor.valueParameters.forEachIndexed { index, _ ->
            putValueArgument(index, original.getValueArgument(index))
        }
    }

    override fun visitEnumConstructorCall(expression: IrEnumConstructorCall): IrExpression {
        val symbol = expression.symbol.replace(SymbolTable::referenceConstructor) ?:
                return super.visitEnumConstructorCall(expression)

        return with(expression) {
            IrEnumConstructorCallImpl(startOffset, endOffset, expression.type, symbol, 0).also {
                it.copyArgumentsFrom(this)
            }
        }
    }

    override fun visitDelegatingConstructorCall(expression: IrDelegatingConstructorCall): IrExpression {
        expression.replaceTypeArguments()

        val symbol = expression.symbol.replace(SymbolTable::referenceConstructor) ?:
                return super.visitDelegatingConstructorCall(expression)

        expression.transformChildrenVoid()
        return with(expression) {
            IrDelegatingConstructorCallImpl(startOffset, endOffset, type, symbol, descriptor, typeArgumentsCount).also {
                it.copyArgumentsFrom(this)
                it.copyTypeArgumentsFrom(this)
            }
        }
    }

    override fun visitFunctionReference(expression: IrFunctionReference): IrExpression {
        expression.replaceTypeArguments()

        val symbol = expression.symbol.replace(SymbolTable::referenceFunction) ?:
                return super.visitFunctionReference(expression)

        expression.transformChildrenVoid(this)
        return with(expression) {
            IrFunctionReferenceImpl(startOffset, endOffset, type, symbol, descriptor, 0).also {
                it.copyArgumentsFrom(this)
                it.copyTypeArgumentsFrom(this)
            }
        }
    }

    override fun visitPropertyReference(expression: IrPropertyReference): IrExpression {
        expression.replaceTypeArguments()

        val field = expression.field?.replaceOrSame(SymbolTable::referenceField)
        val getter = expression.getter?.replace(SymbolTable::referenceSimpleFunction) ?: expression.getter
        val setter = expression.setter?.replace(SymbolTable::referenceSimpleFunction) ?: expression.setter

        if (field == expression.field && getter == expression.getter && setter == expression.setter) {
            return super.visitPropertyReference(expression)
        }

        expression.transformChildrenVoid(this)
        return with(expression) {
            IrPropertyReferenceImpl(startOffset, endOffset, type, descriptor, 0,
                    field,
                    getter,
                    setter,
                    origin).also {

                it.copyArgumentsFrom(this)
                it.copyTypeArgumentsFrom(this)
            }
        }
    }

    override fun visitLocalDelegatedPropertyReference(expression: IrLocalDelegatedPropertyReference): IrExpression {
        val delegate = expression.delegate.replaceOrSame(SymbolTable::referenceVariable)
        val getter = expression.getter.replace(SymbolTable::referenceSimpleFunction) ?: expression.getter
        val setter = expression.setter?.replace(SymbolTable::referenceSimpleFunction) ?: expression.setter

        if (delegate == expression.delegate && getter == expression.getter && setter == expression.setter) {
            return super.visitLocalDelegatedPropertyReference(expression)
        }

        expression.transformChildrenVoid(this)
        return with(expression) {
            IrLocalDelegatedPropertyReferenceImpl(startOffset, endOffset, type, descriptor,
                    delegate, getter, setter, origin).also {

                it.copyArgumentsFrom(this)
            }
        }
    }

    private val returnTargetStack = mutableListOf<IrReturnTargetSymbol>()

    override fun visitFunction(declaration: IrFunction): IrStatement {
        returnTargetStack.push(declaration.symbol)
        try {
            if (declaration is IrSimpleFunction) {
                declaration.overriddenSymbols.forEachIndexed { index, symbol ->
                    val newSymbol = symbol.replace(SymbolTable::referenceSimpleFunction)
                    if (newSymbol != null) {
                        declaration.overriddenSymbols[index] = newSymbol
                    }
                }
            }

            withLocal(declaration.dispatchReceiverParameter?.symbol) {
                withLocal(declaration.extensionReceiverParameter?.symbol) {
                    return super.visitFunction(declaration)
                }
            }
        } finally {
            returnTargetStack.pop()
        }
    }

    override fun visitBlock(expression: IrBlock): IrExpression {
        if (expression is IrReturnableBlock) {
            returnTargetStack.push(expression.symbol)
            try {
                return super.visitBlock(expression)
            } finally {
                returnTargetStack.pop()
            }
        } else {
            return super.visitBlock(expression)
        }
    }

    override fun visitReturn(expression: IrReturn): IrExpression {
        if (expression.returnTargetSymbol.isBound) {
            return super.visitReturn(expression)
        }

        val returnTargetSymbol = returnTargetStack.last { it.descriptor == expression.returnTarget }

        expression.transformChildrenVoid(this)

        return with(expression) {
            IrReturnImpl(startOffset, endOffset, type, returnTargetSymbol, value)
        }
    }

    override fun visitInstanceInitializerCall(expression: IrInstanceInitializerCall): IrExpression {
        val classSymbol = expression.classSymbol.replace(SymbolTable::referenceClass) ?:
                return super.visitInstanceInitializerCall(expression)

        expression.transformChildrenVoid(this)

        return with(expression) {
            IrInstanceInitializerCallImpl(startOffset, endOffset, classSymbol, type)
        }
    }

    override fun visitTypeOperator(expression: IrTypeOperatorCall): IrExpression {
        expression.transformChildrenVoid(this)

        return with(expression) {
            val newTypeOperand = typeOperand /*context.ir.translateErased(typeOperand.toKotlinType())*/
            IrTypeOperatorCallImpl(startOffset, endOffset, type, operator, newTypeOperand).also {
                it.argument = argument
                it.typeOperandClassifier = newTypeOperand.classifierOrFail
            }
        }
    }
}
