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

interface SymbolRemapper {
    fun getDeclaredClass(symbol: IrClassSymbol): IrClassSymbol
    fun getDeclaredFunction(symbol: IrSimpleFunctionSymbol): IrSimpleFunctionSymbol
    fun getDeclaredField(symbol: IrFieldSymbol): IrFieldSymbol
    fun getDeclaredFile(symbol: IrFileSymbol): IrFileSymbol
    fun getDeclaredConstructor(symbol: IrConstructorSymbol): IrConstructorSymbol
    fun getDeclaredEnumEntry(symbol: IrEnumEntrySymbol): IrEnumEntrySymbol
    fun getDeclaredExternalPackageFragment(symbol: IrExternalPackageFragmentSymbol): IrExternalPackageFragmentSymbol
    fun getDeclaredVariable(symbol: IrVariableSymbol): IrVariableSymbol
    fun getDeclaredTypeParameter(symbol: IrTypeParameterSymbol): IrTypeParameterSymbol
    fun getDeclaredValueParameter(symbol: IrValueParameterSymbol): IrValueParameterSymbol
    fun getReferencedClass(symbol: IrClassSymbol): IrClassSymbol
    fun getReferencedClassOrNull(symbol: IrClassSymbol?): IrClassSymbol?
    fun getReferencedEnumEntry(symbol: IrEnumEntrySymbol): IrEnumEntrySymbol
    fun getReferencedVariable(symbol: IrVariableSymbol): IrVariableSymbol
    fun getReferencedField(symbol: IrFieldSymbol): IrFieldSymbol
    fun getReferencedConstructor(symbol: IrConstructorSymbol): IrConstructorSymbol
    fun getReferencedValue(symbol: IrValueSymbol): IrValueSymbol
    fun getReferencedFunction(symbol: IrFunctionSymbol): IrFunctionSymbol
    fun getReferencedSimpleFunction(symbol: IrSimpleFunctionSymbol): IrSimpleFunctionSymbol
    fun getReferencedReturnableBlock(symbol: IrReturnableBlockSymbol): IrReturnableBlockSymbol
    fun getReferencedClassifier(symbol: IrClassifierSymbol): IrClassifierSymbol
}