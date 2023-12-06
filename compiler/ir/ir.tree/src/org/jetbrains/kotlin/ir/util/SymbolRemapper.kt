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

package org.jetbrains.kotlin.ir.util

import org.jetbrains.kotlin.ir.symbols.*

interface SymbolRemapper : DeclaredSymbolRemapper, ReferencedSymbolRemapper {
    open class Empty : SymbolRemapper {
        override fun getDeclaredClass(symbol: IrClassSymbol): IrClassSymbol = symbol
        override fun getDeclaredAnonymousInitializer(symbol: IrAnonymousInitializerSymbol): IrAnonymousInitializerSymbol = symbol

        override fun getDeclaredScript(symbol: IrScriptSymbol): IrScriptSymbol = symbol

        override fun getDeclaredSimpleFunction(symbol: IrSimpleFunctionSymbol): IrSimpleFunctionSymbol = symbol

        override fun getDeclaredProperty(symbol: IrPropertySymbol): IrPropertySymbol = symbol

        override fun getDeclaredField(symbol: IrFieldSymbol): IrFieldSymbol = symbol

        override fun getDeclaredFile(symbol: IrFileSymbol): IrFileSymbol = symbol

        override fun getDeclaredConstructor(symbol: IrConstructorSymbol): IrConstructorSymbol = symbol

        override fun getDeclaredEnumEntry(symbol: IrEnumEntrySymbol): IrEnumEntrySymbol = symbol

        override fun getDeclaredExternalPackageFragment(symbol: IrExternalPackageFragmentSymbol): IrExternalPackageFragmentSymbol = symbol

        override fun getDeclaredVariable(symbol: IrVariableSymbol): IrVariableSymbol = symbol

        override fun getDeclaredLocalDelegatedProperty(symbol: IrLocalDelegatedPropertySymbol): IrLocalDelegatedPropertySymbol = symbol

        override fun getDeclaredTypeParameter(symbol: IrTypeParameterSymbol): IrTypeParameterSymbol = symbol

        override fun getDeclaredValueParameter(symbol: IrValueParameterSymbol): IrValueParameterSymbol = symbol

        override fun getDeclaredTypeAlias(symbol: IrTypeAliasSymbol): IrTypeAliasSymbol = symbol

        override fun getDeclaredReturnableBlock(symbol: IrReturnableBlockSymbol): IrReturnableBlockSymbol = symbol

        override fun getReferencedClass(symbol: IrClassSymbol): IrClassSymbol = symbol

        override fun getReferencedScript(symbol: IrScriptSymbol): IrScriptSymbol = symbol

        override fun getReferencedEnumEntry(symbol: IrEnumEntrySymbol): IrEnumEntrySymbol = symbol

        override fun getReferencedVariable(symbol: IrVariableSymbol): IrVariableSymbol = symbol

        override fun getReferencedLocalDelegatedProperty(symbol: IrLocalDelegatedPropertySymbol): IrLocalDelegatedPropertySymbol = symbol

        override fun getReferencedField(symbol: IrFieldSymbol): IrFieldSymbol = symbol

        override fun getReferencedConstructor(symbol: IrConstructorSymbol): IrConstructorSymbol = symbol

        override fun getReferencedValue(symbol: IrValueSymbol): IrValueSymbol = symbol

        override fun getReferencedFunction(symbol: IrFunctionSymbol): IrFunctionSymbol = symbol

        override fun getReferencedProperty(symbol: IrPropertySymbol): IrPropertySymbol = symbol

        override fun getReferencedSimpleFunction(symbol: IrSimpleFunctionSymbol): IrSimpleFunctionSymbol = symbol

        override fun getReferencedClassifier(symbol: IrClassifierSymbol): IrClassifierSymbol = symbol

        override fun getReferencedReturnTarget(symbol: IrReturnTargetSymbol): IrReturnTargetSymbol = symbol

        override fun getReferencedTypeAlias(symbol: IrTypeAliasSymbol): IrTypeAliasSymbol = symbol
    }

    companion object {
        val EMPTY: SymbolRemapper = Empty()
    }
}
