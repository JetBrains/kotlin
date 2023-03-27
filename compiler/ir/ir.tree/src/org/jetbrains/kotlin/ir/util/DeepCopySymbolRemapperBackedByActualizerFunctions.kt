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

import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.symbols.*

open class DeepCopySymbolRemapperBackedByActualizerFunctions(
    private val classActualizer: (IrClass) -> IrClass?,
    private val functionActualizer: (IrFunction) -> IrFunction?,
    descriptorsRemapper: DescriptorsRemapper = NullDescriptorsRemapper,
) : DeepCopySymbolRemapper(descriptorsRemapper) {

    override fun getReferencedClass(symbol: IrClassSymbol): IrClassSymbol = classes[symbol]
        ?: classActualizer(symbol.owner)?.symbol
        ?: symbol

    override fun getReferencedClassOrNull(symbol: IrClassSymbol?): IrClassSymbol? = symbol?.let { getReferencedClass(it) }

    override fun getReferencedConstructor(symbol: IrConstructorSymbol): IrConstructorSymbol = constructors[symbol]
        ?: functionActualizer(symbol.owner)?.symbol as? IrConstructorSymbol
        ?: symbol

    override fun getReferencedSimpleFunction(symbol: IrSimpleFunctionSymbol): IrSimpleFunctionSymbol = functions[symbol]
        ?: functionActualizer(symbol.owner)?.symbol as? IrSimpleFunctionSymbol
        ?: symbol

    override fun getReferencedFunction(symbol: IrFunctionSymbol): IrFunctionSymbol =
        when (symbol) {
            is IrSimpleFunctionSymbol -> getReferencedSimpleFunction(symbol)
            is IrConstructorSymbol -> getReferencedConstructor(symbol)
            else -> throw IllegalArgumentException("Unexpected symbol $symbol")
        }

    override fun getReferencedClassifier(symbol: IrClassifierSymbol): IrClassifierSymbol =
        when (symbol) {
            is IrClassSymbol -> getReferencedClass(symbol)
            else -> super.getReferencedClassifier(symbol)
        }
}