/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.actualizer

import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrMemberAccessExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrConstructorCallImpl
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.util.DeepCopyIrTreeWithSymbols
import org.jetbrains.kotlin.ir.util.SymbolRemapper
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.utils.memoryOptimizedMap

internal class ActualizerSymbolRemapper(private val expectActualMap: Map<IrSymbol, IrSymbol>) : SymbolRemapper {
    override fun getDeclaredClass(symbol: IrClassSymbol) = symbol

    override fun getDeclaredAnonymousInitializer(symbol: IrAnonymousInitializerSymbol) = symbol

    override fun getDeclaredScript(symbol: IrScriptSymbol) = symbol

    override fun getDeclaredSimpleFunction(symbol: IrSimpleFunctionSymbol) = symbol

    override fun getDeclaredProperty(symbol: IrPropertySymbol) = symbol

    override fun getDeclaredField(symbol: IrFieldSymbol) = symbol

    override fun getDeclaredFile(symbol: IrFileSymbol) = symbol

    override fun getDeclaredConstructor(symbol: IrConstructorSymbol) = symbol

    override fun getDeclaredEnumEntry(symbol: IrEnumEntrySymbol) = symbol

    override fun getDeclaredExternalPackageFragment(symbol: IrExternalPackageFragmentSymbol) = symbol

    override fun getDeclaredVariable(symbol: IrVariableSymbol) = symbol

    override fun getDeclaredLocalDelegatedProperty(symbol: IrLocalDelegatedPropertySymbol) = symbol

    override fun getDeclaredTypeParameter(symbol: IrTypeParameterSymbol) = symbol

    override fun getDeclaredValueParameter(symbol: IrValueParameterSymbol) = symbol

    override fun getDeclaredTypeAlias(symbol: IrTypeAliasSymbol) = symbol

    override fun getDeclaredReturnableBlock(symbol: IrReturnableBlockSymbol): IrReturnableBlockSymbol = symbol

    override fun getReferencedClass(symbol: IrClassSymbol) = symbol.actualizeSymbol()

    override fun getReferencedScript(symbol: IrScriptSymbol) = symbol.actualizeSymbol()

    override fun getReferencedEnumEntry(symbol: IrEnumEntrySymbol) = symbol.actualizeSymbol()

    override fun getReferencedVariable(symbol: IrVariableSymbol) = symbol.actualizeSymbol()

    override fun getReferencedLocalDelegatedProperty(symbol: IrLocalDelegatedPropertySymbol) = symbol.actualizeSymbol()

    override fun getReferencedField(symbol: IrFieldSymbol) = symbol.actualizeSymbol()

    override fun getReferencedConstructor(symbol: IrConstructorSymbol) = symbol.actualizeSymbol()

    override fun getReferencedValue(symbol: IrValueSymbol) = symbol.actualizeSymbol()

    override fun getReferencedValueParameter(symbol: IrValueParameterSymbol) = symbol.actualizeSymbol<IrValueSymbol>()

    override fun getReferencedFunction(symbol: IrFunctionSymbol) = symbol.actualizeSymbol()

    override fun getReferencedProperty(symbol: IrPropertySymbol) = symbol.actualizeSymbol()

    override fun getReferencedSimpleFunction(symbol: IrSimpleFunctionSymbol) = symbol.actualizeSymbol()

    override fun getReferencedClassifier(symbol: IrClassifierSymbol) = symbol.actualizeSymbol()

    override fun getReferencedTypeParameter(symbol: IrTypeParameterSymbol) = symbol.actualizeSymbol<IrClassifierSymbol>()

    override fun getReferencedReturnTarget(symbol: IrReturnTargetSymbol) = symbol.actualizeSymbol()

    override fun getReferencedReturnableBlock(symbol: IrReturnableBlockSymbol) = symbol.actualizeSymbol<IrReturnTargetSymbol>()

    override fun getReferencedTypeAlias(symbol: IrTypeAliasSymbol) = symbol.actualizeSymbol()

    private inline fun <reified S : IrSymbol> S.actualizeSymbol(): S {
        val actualSymbol = expectActualMap[this] ?: return this
        return actualSymbol as? S
            ?: error("Unexpected type of actual symbol. Expected: ${S::class.java.simpleName}, got ${actualSymbol.javaClass.simpleName}")
    }
}

internal open class ActualizerVisitor(private val symbolRemapper: SymbolRemapper) : DeepCopyIrTreeWithSymbols(symbolRemapper) {
    // We shouldn't touch attributes, because Fir2Ir wouldn't set them to anything meaningful anyway.
    // So it would be better to have them as is, i.e. referring to `this`, not some random node removed from the tree
    override fun <D : IrAttributeContainer> D.processAttributes(other: IrAttributeContainer?): D = this

    override fun visitModuleFragment(declaration: IrModuleFragment) =
        declaration.also { it.transformChildren(this, null) }

    override fun visitExternalPackageFragment(declaration: IrExternalPackageFragment) =
        declaration.also { it.transformChildren(this, null) }

    override fun visitFile(declaration: IrFile) =
        declaration.also {
            it.transformChildren(this, null)
            it.transformAnnotations(declaration)
        }

    override fun visitScript(declaration: IrScript) =
        declaration.also {
            it.baseClass = it.baseClass?.remapType()
            it.transformChildren(this, null)
        }

    override fun visitClass(declaration: IrClass) =
        declaration.also {
            if (declaration.isExpect) return@also
            it.superTypes = it.superTypes.map { superType -> superType.remapType() }
            it.transformChildren(this, null)
            it.transformAnnotations(declaration)
            it.valueClassRepresentation = it.valueClassRepresentation?.mapUnderlyingType { type ->
                type.remapType() as? IrSimpleType ?: error("Value class underlying type is not a simple type: ${it.render()}")
            }
        }

    override fun visitSimpleFunction(declaration: IrSimpleFunction) = (visitFunction(declaration) as IrSimpleFunction).also {
        if (declaration.isExpect) return@also
        it.overriddenSymbols = it.overriddenSymbols.memoryOptimizedMap { symbol ->
            symbolRemapper.getReferencedFunction(symbol) as IrSimpleFunctionSymbol
        }
    }

    override fun visitConstructor(declaration: IrConstructor) = visitFunction(declaration) as IrConstructor

    override fun visitFunction(declaration: IrFunction) =
        declaration.also {
            if (declaration.isExpect) return@also
            it.returnType = it.returnType.remapType()
            it.transformChildren(this, null)
            it.transformAnnotations(declaration)
        }

    override fun visitProperty(declaration: IrProperty) =
        declaration.also {
            if (declaration.isExpect) return@also
            it.transformChildren(this, null)
            it.overriddenSymbols = it.overriddenSymbols.memoryOptimizedMap { symbol ->
                symbolRemapper.getReferencedProperty(symbol)
            }
            it.transformAnnotations(declaration)
        }

    override fun visitField(declaration: IrField) =
        declaration.also {
            it.type = it.type.remapType()
            it.transformChildren(this, null)
            it.transformAnnotations(declaration)
        }

    override fun visitLocalDelegatedProperty(declaration: IrLocalDelegatedProperty) =
        declaration.also {
            it.type = it.type.remapType()
            it.transformChildren(this, null)
        }

    override fun visitEnumEntry(declaration: IrEnumEntry) =
        declaration.also {
            it.transformChildren(this, null)
            it.transformAnnotations(declaration)
        }

    override fun visitTypeParameter(declaration: IrTypeParameter) =
        declaration.also {
            it.superTypes = it.superTypes.map { superType -> superType.remapType() }
            it.transformChildren(this, null)
            it.transformAnnotations(declaration)
        }

    override fun visitValueParameter(declaration: IrValueParameter) =
        declaration.also {
            it.type = it.type.remapType()
            it.varargElementType = it.varargElementType?.remapType()
            it.transformChildren(this, null)
            it.transformAnnotations(declaration)
        }

    override fun visitAnonymousInitializer(declaration: IrAnonymousInitializer) =
        declaration.also { it.transformChildren(this, null) }

    override fun visitVariable(declaration: IrVariable) =
        declaration.also {
            it.type = it.type.remapType()
            it.transformChildren(this, null)
            it.transformAnnotations(declaration)
        }

    override fun visitTypeAlias(declaration: IrTypeAlias) =
        declaration.also {
            it.expandedType = it.expandedType.remapType()
            it.transformChildren(this, null)
            it.transformAnnotations(declaration)
        }

    // FIXME(KT-67752): This is copied verbatim from DeepCopyIrTreeWithSymbols
    // We could make the method in DeepCopyIrTreeWithSymbols protected instead of private, but that will break compilation of
    // the Compose plugin
    private fun <T : IrMemberAccessExpression<*>> T.transformReceiverArguments(original: T): T =
        apply {
            dispatchReceiver = original.dispatchReceiver?.transform()
            extensionReceiver = original.extensionReceiver?.transform()
        }

    // FIXME(KT-67752): This is copied verbatim from DeepCopyIrTreeWithSymbols
    // We could make the method in DeepCopyIrTreeWithSymbols protected instead of private, but that will break compilation of
    // the Compose plugin
    private fun IrMemberAccessExpression<*>.copyRemappedTypeArgumentsFrom(other: IrMemberAccessExpression<*>) {
        assert(typeArgumentsCount == other.typeArgumentsCount) {
            "Mismatching type arguments: $typeArgumentsCount vs ${other.typeArgumentsCount} "
        }
        for (i in 0 until typeArgumentsCount) {
            putTypeArgument(i, other.getTypeArgument(i)?.remapType())
        }
    }

    // FIXME(KT-67752): This is copied verbatim from DeepCopyIrTreeWithSymbols
    // We could make the method in DeepCopyIrTreeWithSymbols protected instead of private, but that will break compilation of
    // the Compose plugin
    private fun <T : IrMemberAccessExpression<*>> T.transformValueArguments(original: T) {
        transformReceiverArguments(original)
        for (i in 0 until original.valueArgumentsCount) {
            putValueArgument(i, original.getValueArgument(i)?.transform())
        }
    }

    override fun visitConstructorCall(expression: IrConstructorCall): IrConstructorCall {
        val constructorSymbol = symbolRemapper.getReferencedConstructor(expression.symbol)

        // This is a hack to allow actualizing annotation constructors without parameters with constructors with default arguments.
        // Without it, attempting to call such a constructor in common code will result in either a backend exception or in linkage error.
        // See KT-67488 for details.
        val valueArgumentsCount =
            if (constructorSymbol.isBound) constructorSymbol.owner.valueParameters.size else expression.valueArgumentsCount

        return IrConstructorCallImpl(
            expression.startOffset,
            expression.endOffset,
            expression.type.remapType(),
            constructorSymbol,
            expression.typeArgumentsCount,
            expression.constructorTypeArgumentsCount,
            valueArgumentsCount,
            mapStatementOrigin(expression.origin),
        ).apply {
            copyRemappedTypeArgumentsFrom(expression)
            transformValueArguments(expression)
        }.processAttributes(expression)
    }
}