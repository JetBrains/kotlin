/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi2ir.generators.fragments

import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ParameterDescriptor
import org.jetbrains.kotlin.descriptors.ReceiverParameterDescriptor
import org.jetbrains.kotlin.descriptors.ValueDescriptor
import org.jetbrains.kotlin.ir.declarations.IrFactory
import org.jetbrains.kotlin.ir.symbols.IrValueParameterSymbol
import org.jetbrains.kotlin.ir.symbols.IrValueSymbol
import org.jetbrains.kotlin.ir.util.IdSignatureComposer
import org.jetbrains.kotlin.ir.util.NameProvider
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.resolve.scopes.receivers.ExtensionReceiver
import org.jetbrains.kotlin.resolve.scopes.receivers.ThisClassReceiver

// Used from CodeFragmentCompiler for IDE Debugger Plug-In
@Suppress("unused")
class FragmentCompilerSymbolTableDecorator(
    signatureComposer: IdSignatureComposer,
    irFactory: IrFactory,
    private val fragmentInfo: EvaluatorFragmentInfo,
    nameProvider: NameProvider = NameProvider.DEFAULT,
) : SymbolTable(signatureComposer, irFactory, nameProvider) {

    override fun referenceValueParameter(descriptor: ParameterDescriptor): IrValueParameterSymbol {
        if (descriptor !is ReceiverParameterDescriptor) return super.referenceValueParameter(descriptor)

        val finderPredicate = when (val receiverValue = descriptor.value) {
            is ExtensionReceiver -> { (targetDescriptor, _): EvaluatorFragmentParameterInfo ->
                receiverValue == (targetDescriptor as? ReceiverParameterDescriptor)?.value
            }
            is ThisClassReceiver -> { (targetDescriptor, _): EvaluatorFragmentParameterInfo ->
                receiverValue.classDescriptor == targetDescriptor.original
            }
            else -> TODO("Unimplemented")
        }

        val parameterPosition =
            fragmentInfo.parameters.indexOfFirst(finderPredicate)
        if (parameterPosition > -1) {
            return super.referenceValueParameter(fragmentInfo.methodDescriptor.valueParameters[parameterPosition])
        }
        return super.referenceValueParameter(descriptor)
    }

    override fun referenceValue(value: ValueDescriptor): IrValueSymbol {
        val parameterPosition =
            fragmentInfo.parameters.indexOfFirst { it.descriptor == value }
        if (parameterPosition > -1) {
            return super.referenceValueParameter(fragmentInfo.methodDescriptor.valueParameters[parameterPosition])
        }

        return super.referenceValue(value)
    }
}