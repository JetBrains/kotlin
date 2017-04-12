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

package org.jetbrains.kotlin.ir.descriptors

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.DeclarationDescriptorWithSource
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.ParameterDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrTypeParameterImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrValueParameterImpl
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.symbols.IrValueParameterSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrTypeParameterSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrValueParameterSymbolImpl
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.resolve.source.PsiSourceElement
import org.jetbrains.kotlin.utils.addIfNotNull
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

class IrDeclarationStubBuilder(val defaultOrigin: IrDeclarationOrigin) {
    fun buildSimpleFunctionStub(symbol: IrSimpleFunctionSymbol, origin: IrDeclarationOrigin = defaultOrigin): IrSimpleFunction =
            IrFunctionImpl(symbol.descriptor.startOffset, symbol.descriptor.endOffset, origin, symbol)
                    .buildTypeParameterStubs(symbol.descriptor.typeParameters, origin)
                    .buildValueParameterStubs(symbol.descriptor, origin)

    fun buildTypeParameterStub(symbol: IrTypeParameterSymbol, origin: IrDeclarationOrigin = defaultOrigin): IrTypeParameter =
            IrTypeParameterImpl(symbol.descriptor.startOffset, symbol.descriptor.endOffset, origin, symbol)

    fun buildValueParameterStub(symbol: IrValueParameterSymbol, origin: IrDeclarationOrigin = defaultOrigin): IrValueParameter =
            IrValueParameterImpl(symbol.descriptor.startOffset, symbol.descriptor.endOffset, origin, symbol)

    private fun <T : IrTypeParametersContainer>
    T.buildTypeParameterStubs(typeParameterDescriptors: List<TypeParameterDescriptor>, origin: IrDeclarationOrigin): T =
            apply {
                typeParameterDescriptors.mapTo(typeParameters) { buildTypeParameterStub(IrTypeParameterSymbolImpl(it), origin) }
            }

    private fun <T : IrFunction>
    T.buildValueParameterStubs(functionDescriptor: FunctionDescriptor, origin: IrDeclarationOrigin): T =
            apply {
                val valueParameterDescriptors = ArrayList<ParameterDescriptor>(functionDescriptor.valueParameters.size + 2).apply {
                    addIfNotNull(functionDescriptor.dispatchReceiverParameter)
                    addIfNotNull(functionDescriptor.extensionReceiverParameter)
                    addAll(functionDescriptor.valueParameters)
                }
                valueParameterDescriptors.mapTo(valueParameters) { buildValueParameterStub(IrValueParameterSymbolImpl(it), origin) }
            }

    private val DeclarationDescriptorWithSource.startOffset
        get() = psiElement?.startOffset ?: UNDEFINED_OFFSET

    private val DeclarationDescriptorWithSource.endOffset
        get() = psiElement?.startOffset ?: UNDEFINED_OFFSET

    private val DeclarationDescriptorWithSource.psiElement: PsiElement?
        get() = source.safeAs<PsiSourceElement>()?.psi
}