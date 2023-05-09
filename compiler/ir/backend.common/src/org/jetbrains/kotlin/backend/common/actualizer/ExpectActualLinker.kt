/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.actualizer

import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.util.*

internal class ActualizerSymbolRemapper(private val expectActualMap: Map<IrSymbol, IrSymbol>) : SymbolRemapper {
    override fun getDeclaredClass(symbol: IrClassSymbol) = symbol

    override fun getDeclaredScript(symbol: IrScriptSymbol) = symbol

    override fun getDeclaredFunction(symbol: IrSimpleFunctionSymbol) = symbol

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

    override fun getReferencedClass(symbol: IrClassSymbol) = symbol.actualizeSymbol()

    override fun getReferencedScript(symbol: IrScriptSymbol) = symbol.actualizeSymbol()

    override fun getReferencedClassOrNull(symbol: IrClassSymbol?) = symbol?.actualizeSymbol()

    override fun getReferencedEnumEntry(symbol: IrEnumEntrySymbol) = symbol.actualizeSymbol()

    override fun getReferencedVariable(symbol: IrVariableSymbol) = symbol.actualizeSymbol()

    override fun getReferencedLocalDelegatedProperty(symbol: IrLocalDelegatedPropertySymbol) = symbol.actualizeSymbol()

    override fun getReferencedField(symbol: IrFieldSymbol) = symbol.actualizeSymbol()

    override fun getReferencedConstructor(symbol: IrConstructorSymbol) = symbol.actualizeSymbol()

    override fun getReferencedValue(symbol: IrValueSymbol) = symbol.actualizeSymbol()

    override fun getReferencedFunction(symbol: IrFunctionSymbol) = symbol.actualizeSymbol()

    override fun getReferencedProperty(symbol: IrPropertySymbol) = symbol.actualizeSymbol()

    override fun getReferencedSimpleFunction(symbol: IrSimpleFunctionSymbol) = symbol.actualizeSymbol()

    override fun getReferencedReturnableBlock(symbol: IrReturnableBlockSymbol) = symbol.actualizeSymbol()

    override fun getReferencedClassifier(symbol: IrClassifierSymbol) = symbol.actualizeSymbol()

    override fun getReferencedTypeAlias(symbol: IrTypeAliasSymbol) = symbol.actualizeSymbol()

    private inline fun <reified S : IrSymbol> S.actualizeSymbol(): S = (expectActualMap[this] as? S) ?: this
}

internal open class ActualizerVisitor(symbolRemapper: SymbolRemapper, typeRemapper: TypeRemapper) :
    DeepCopyIrTreeWithSymbols(symbolRemapper, typeRemapper, SymbolRenamer.DEFAULT) {
    override fun visitModuleFragment(declaration: IrModuleFragment) =
        declaration.also { it.transformChildren(this, null) }

    override fun visitExternalPackageFragment(declaration: IrExternalPackageFragment) =
        declaration.also { it.transformChildren(this, null) }

    override fun visitFile(declaration: IrFile) =
        declaration.also { it.transformChildren(this, null) }

    override fun visitScript(declaration: IrScript) =
        declaration.also {
            it.baseClass = it.baseClass?.remapType()
            it.transformChildren(this, null)
        }

    override fun visitClass(declaration: IrClass) =
        declaration.also {
            it.superTypes = it.superTypes.map { superType -> superType.remapType() }
            it.transformChildren(this, null)
        }

    override fun visitSimpleFunction(declaration: IrSimpleFunction) = visitFunction(declaration) as IrSimpleFunction

    override fun visitConstructor(declaration: IrConstructor) = visitFunction(declaration) as IrConstructor

    override fun visitFunction(declaration: IrFunction) =
        declaration.also {
            it.returnType = it.returnType.remapType()
            it.transformChildren(this, null)
        }

    override fun visitProperty(declaration: IrProperty) =
        declaration.also { it.transformChildren(this, null) }

    override fun visitField(declaration: IrField) =
        declaration.also {
            it.type = it.type.remapType()
            it.transformChildren(this, null)
        }

    override fun visitLocalDelegatedProperty(declaration: IrLocalDelegatedProperty) =
        declaration.also {
            it.type = it.type.remapType()
            it.transformChildren(this, null)
        }

    override fun visitEnumEntry(declaration: IrEnumEntry) =
        declaration.also { it.transformChildren(this, null) }

    override fun visitTypeParameter(declaration: IrTypeParameter) =
        declaration.also {
            it.superTypes = it.superTypes.map { superType -> superType.remapType() }
            it.transformChildren(this, null)
        }

    override fun visitValueParameter(declaration: IrValueParameter) =
        declaration.also {
            it.type = it.type.remapType()
            it.varargElementType = it.varargElementType?.remapType()
            it.transformChildren(this, null)
        }

    override fun visitAnonymousInitializer(declaration: IrAnonymousInitializer) =
        declaration.also { it.transformChildren(this, null) }

    override fun visitVariable(declaration: IrVariable) =
        declaration.also {
            it.type = it.type.remapType()
            it.transformChildren(this, null)
        }

    override fun visitTypeAlias(declaration: IrTypeAlias) =
        declaration.also {
            it.expandedType = it.expandedType.remapType()
            it.transformChildren(this, null)
        }
}