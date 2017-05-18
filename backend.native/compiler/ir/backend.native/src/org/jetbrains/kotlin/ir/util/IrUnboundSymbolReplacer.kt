package org.jetbrains.kotlin.ir.util

import org.jetbrains.kotlin.backend.common.pop
import org.jetbrains.kotlin.backend.common.push
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.ir.IrReturnableBlock
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrAnonymousInitializer
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrSymbolOwner
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.IrBindableSymbol
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.visitors.*
import org.jetbrains.kotlin.psi2ir.generators.ModuleDependenciesGenerator
import org.jetbrains.kotlin.psi2ir.generators.SymbolTable

@Deprecated("")
internal fun IrModuleFragment.replaceUnboundSymbols(context: Context) {
    val collector = DeclarationSymbolCollector()
    with(collector) {
        with(irBuiltins) {
            for (op in arrayOf(eqeqeqFun, eqeqFun, lt0Fun, lteq0Fun, gt0Fun, gteq0Fun, throwNpeFun, booleanNotFun,
                    noWhenBranchMatchedExceptionFun)) {

                register(op.symbol)
            }
        }
    }
    this.acceptVoid(collector)

    val symbolTable = context.ir.symbols.symbolTable

    this.transformChildrenVoid(IrUnboundSymbolReplacer(symbolTable, collector.descriptorToSymbol))

    // Generate missing external stubs:
    ModuleDependenciesGenerator(context.psi2IrGeneratorContext).generateUnboundSymbolsAsDependencies(this)

    // Merge duplicated module and package declarations:
    this.acceptVoid(object : IrElementVisitorVoid {
        override fun visitElement(element: IrElement) {}

        override fun visitModuleFragment(declaration: IrModuleFragment) {
            declaration.dependencyModules.forEach { it.acceptVoid(this) }

            val dependencyModules = declaration.dependencyModules.groupBy { it.descriptor }.map { (_, fragments) ->
                fragments.reduce { firstModule, nextModule ->
                    firstModule.apply {
                        mergeFrom(nextModule)
                    }
                }
            }

            declaration.dependencyModules.clear()
            declaration.dependencyModules.addAll(dependencyModules)
        }

    })
}

private fun IrModuleFragment.mergeFrom(other: IrModuleFragment): Unit {
    assert(this.files.isEmpty())
    assert(other.files.isEmpty())

    val thisPackages = this.externalPackageFragments.groupBy { it.packageFragmentDescriptor }
    other.externalPackageFragments.forEach {
        val thisPackage = thisPackages[it.packageFragmentDescriptor]?.single()
        if (thisPackage == null) {
            this.externalPackageFragments.add(it)
        } else {
            thisPackage.declarations.addAll(it.declarations)
        }
    }
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

    private inline fun <D : DeclarationDescriptor, reified S : IrBindableSymbol<D, *>> S.replace(
            referenceSymbol: (SymbolTable, D) -> S): S? {

        if (this.isBound) {
            return null
        }

        descriptorToSymbol[this.descriptor]?.let {
            return it as S
        }

        return referenceSymbol(symbolTable, this.descriptor)
    }

    private inline fun <D : DeclarationDescriptor, reified S : IrBindableSymbol<D, *>> S.replaceOrSame(
            referenceSymbol: (SymbolTable, D) -> S): S = this.replace(referenceSymbol) ?: this

    private fun IrFunctionSymbol.replace(
            referenceSymbol: (SymbolTable, FunctionDescriptor) -> IrFunctionSymbol): IrFunctionSymbol? {

        if (this.isBound) {
            return null
        }

        descriptorToSymbol[this.descriptor]?.let {
            return it as IrFunctionSymbol
        }

        return referenceSymbol(symbolTable, this.descriptor)
    }

    private inline fun <reified S : IrSymbol> S.replaceLocal(): S? {
        return if (this.isBound) {
            null
        } else {
            descriptorToSymbol[this.descriptor] as S
        }
    }

    override fun visitGetValue(expression: IrGetValue): IrExpression {
        val symbol = expression.symbol.replaceLocal() ?: return super.visitGetValue(expression)

        expression.transformChildrenVoid(this)
        return with(expression) {
            IrGetValueImpl(startOffset, endOffset, symbol, origin)
        }
    }

    override fun visitSetVariable(expression: IrSetVariable): IrExpression {
        val symbol = expression.symbol.replaceLocal() ?: return super.visitSetVariable(expression)

        expression.transformChildrenVoid(this)
        return with(expression) {
            IrSetVariableImpl(startOffset, endOffset, symbol, value, origin)
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
        val symbol = expression.symbol.let {
            if (it.isBound) {
                return super.visitClassReference(expression)
            }

            descriptorToSymbol[it.descriptor]?.let {
                it as IrClassifierSymbol
            }

            symbolTable.referenceClassifier(it.descriptor)
        }

        expression.transformChildrenVoid(this)
        return with(expression) {
            IrClassReferenceImpl(startOffset, endOffset, type, symbol)
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
            IrGetFieldImpl(startOffset, endOffset, symbol, receiver, origin, superQualifierSymbol)
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
            IrSetFieldImpl(startOffset, endOffset, symbol, receiver, value, origin, superQualifierSymbol)
        }
    }

    override fun visitCall(expression: IrCall): IrExpression {
        val symbol = expression.symbol.replace(SymbolTable::referenceFunction) ?: expression.symbol

        val superQualifierSymbol = expression.superQualifierSymbol?.replaceOrSame(SymbolTable::referenceClass)

        if (symbol == expression.symbol && superQualifierSymbol == expression.superQualifierSymbol) {
            return super.visitCall(expression)
        }

        expression.transformChildrenVoid()
        return with(expression) {
            IrCallImpl(startOffset, endOffset, symbol, descriptor,
                    getTypeArgumentsMap(),
                    origin, superQualifierSymbol).also {

                it.copyArgumentsFrom(this)
            }
        }
    }

    private fun IrMemberAccessExpression.getTypeArgumentsMap() =
            descriptor.original.typeParameters.associate { it to getTypeArgumentOrDefault(it) }

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
            IrEnumConstructorCallImpl(startOffset, endOffset, symbol).also {
                it.copyArgumentsFrom(this)
            }
        }
    }

    override fun visitDelegatingConstructorCall(expression: IrDelegatingConstructorCall): IrExpression {
        val symbol = expression.symbol.replace(SymbolTable::referenceConstructor) ?:
                return super.visitDelegatingConstructorCall(expression)

        expression.transformChildrenVoid()
        return with(expression) {
            IrDelegatingConstructorCallImpl(startOffset, endOffset, symbol, descriptor, getTypeArgumentsMap()).also {
                it.copyArgumentsFrom(this)
            }
        }
    }

    override fun visitFunctionReference(expression: IrFunctionReference): IrExpression {
        val symbol = expression.symbol.replace(SymbolTable::referenceFunction) ?:
                return super.visitFunctionReference(expression)

        expression.transformChildrenVoid(this)
        return with(expression) {
            IrFunctionReferenceImpl(startOffset, endOffset, type, symbol, descriptor, getTypeArgumentsMap()).also {
                it.copyArgumentsFrom(this)
            }
        }
    }

    override fun visitPropertyReference(expression: IrPropertyReference): IrExpression {
        val field = expression.field?.replaceOrSame(SymbolTable::referenceField)
        val getter = expression.getter?.replace(SymbolTable::referenceFunction) ?: expression.getter
        val setter = expression.setter?.replace(SymbolTable::referenceFunction) ?: expression.setter

        if (field == expression.field && getter == expression.getter && setter == expression.setter) {
            return super.visitPropertyReference(expression)
        }

        expression.transformChildrenVoid(this)
        return with(expression) {
            IrPropertyReferenceImpl(startOffset, endOffset, type, descriptor,
                    field,
                    getter,
                    setter,
                    getTypeArgumentsMap(), origin).also {

                it.copyArgumentsFrom(this)
            }
        }
    }

    override fun visitLocalDelegatedPropertyReference(expression: IrLocalDelegatedPropertyReference): IrExpression {
        val delegate = expression.delegate.replaceOrSame(SymbolTable::referenceVariable)
        val getter = expression.getter.replace(SymbolTable::referenceFunction) ?: expression.getter
        val setter = expression.setter?.replace(SymbolTable::referenceFunction) ?: expression.setter

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

    private val returnTargetStack = mutableListOf<IrFunctionSymbol>()

    override fun visitFunction(declaration: IrFunction): IrStatement {
        returnTargetStack.push(declaration.symbol)
        try {
            return super.visitFunction(declaration)
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
            IrInstanceInitializerCallImpl(startOffset, endOffset, classSymbol)
        }
    }
}
