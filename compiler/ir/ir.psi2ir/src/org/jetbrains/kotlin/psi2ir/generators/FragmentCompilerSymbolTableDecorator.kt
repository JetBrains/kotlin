/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi2ir.generators

import org.jetbrains.kotlin.codegen.CodeFragmentCodegenInfo
import org.jetbrains.kotlin.descriptors.ParameterDescriptor
import org.jetbrains.kotlin.descriptors.ReceiverParameterDescriptor
import org.jetbrains.kotlin.descriptors.ValueDescriptor
import org.jetbrains.kotlin.ir.declarations.IrFactory
import org.jetbrains.kotlin.ir.symbols.IrValueParameterSymbol
import org.jetbrains.kotlin.ir.symbols.IrValueSymbol
import org.jetbrains.kotlin.ir.util.IdSignatureComposer
import org.jetbrains.kotlin.ir.util.NameProvider
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.resolve.scopes.receivers.ImplicitClassReceiver

class FragmentCompilerSymbolTableDecorator(
    signatureComposer: IdSignatureComposer,
    irFactory: IrFactory,
    nameProvider: NameProvider = NameProvider.DEFAULT,
    private val fragmentInfo: CodeFragmentCodegenInfo
) : SymbolTable(signatureComposer, irFactory, nameProvider) {

    override fun referenceValueParameter(descriptor: ParameterDescriptor): IrValueParameterSymbol {
        //TODO(kjaa): this cast feels _waaaaaay_ specific. maybe a dispatch, here? Or simply just case-by-case it.
        return if (descriptor is ReceiverParameterDescriptor) {
            val parameterPosition =
                fragmentInfo.parameters.map { it.targetDescriptor }.indexOf((descriptor.value as ImplicitClassReceiver).classDescriptor)
            return if (parameterPosition > -1) {
                super.referenceValueParameter(fragmentInfo.methodDescriptor.valueParameters[parameterPosition])
            } else {
                super.referenceValueParameter(descriptor)
            }
        } else {
            super.referenceValueParameter(descriptor)
        }
    }

    override fun referenceValue(value: ValueDescriptor): IrValueSymbol {
        val parameterPosition =
            fragmentInfo.parameters.map { it.targetDescriptor }.indexOf(value)
        return if (parameterPosition > -1) {
            super.referenceValueParameter(fragmentInfo.methodDescriptor.valueParameters[parameterPosition])
        } else {
            super.referenceValue(value)
        }
    }
}