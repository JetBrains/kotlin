/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi2ir.generators.fragments

import org.jetbrains.kotlin.descriptors.ParameterDescriptor
import org.jetbrains.kotlin.descriptors.ReceiverParameterDescriptor
import org.jetbrains.kotlin.descriptors.ValueDescriptor
import org.jetbrains.kotlin.descriptors.impl.AbstractReceiverParameterDescriptor
import org.jetbrains.kotlin.ir.declarations.IrFactory
import org.jetbrains.kotlin.ir.symbols.IrValueParameterSymbol
import org.jetbrains.kotlin.ir.symbols.IrValueSymbol
import org.jetbrains.kotlin.ir.util.DescriptorSymbolTableExtension
import org.jetbrains.kotlin.ir.util.IdSignatureComposer
import org.jetbrains.kotlin.ir.util.NameProvider
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.resolve.scopes.receivers.ContextReceiver
import org.jetbrains.kotlin.resolve.scopes.receivers.ExtensionReceiver
import org.jetbrains.kotlin.resolve.scopes.receivers.ThisClassReceiver

// Used from CodeFragmentCompiler for IDE Debugger Plug-In
@Suppress("unused")
class FragmentCompilerSymbolTableDecorator(
    signatureComposer: IdSignatureComposer,
    irFactory: IrFactory,
    var fragmentInfo: EvaluatorFragmentInfo?,
    nameProvider: NameProvider = NameProvider.DEFAULT,
) : SymbolTable(signatureComposer, irFactory, nameProvider) {

    override fun createDescriptorExtension(): DescriptorSymbolTableExtension {
        return ExtensionDecorator()
    }

    private inner class ExtensionDecorator : DescriptorSymbolTableExtension(this) {
        override fun referenceValueParameter(declaration: ParameterDescriptor): IrValueParameterSymbol {
            val fi = fragmentInfo ?: return super.referenceValueParameter(declaration)

            if (declaration !is ReceiverParameterDescriptor) return super.referenceValueParameter(declaration)

            val finderPredicate = when (val receiverValue = declaration.value) {
                is ExtensionReceiver, is ContextReceiver -> { (targetDescriptor, _): EvaluatorFragmentParameterInfo ->
                    receiverValue == (targetDescriptor as? ReceiverParameterDescriptor)?.value
                }
                is ThisClassReceiver -> { (targetDescriptor, _): EvaluatorFragmentParameterInfo ->
                    receiverValue.classDescriptor == targetDescriptor.original
                }
                else -> TODO("Unimplemented")
            }

            val parameterPosition =
                fi.parameters.indexOfFirst(finderPredicate)
            if (parameterPosition > -1) {
                return super.referenceValueParameter(fi.methodDescriptor.valueParameters[parameterPosition])
            }
            return super.referenceValueParameter(declaration)
        }

        override fun referenceValue(value: ValueDescriptor): IrValueSymbol {
            val fi = fragmentInfo ?: return super.referenceValue(value)

            val finderPredicate = when (value) {
                is AbstractReceiverParameterDescriptor -> { (targetDescriptor, _): EvaluatorFragmentParameterInfo ->
                    value.containingDeclaration == targetDescriptor
                }
                else -> { (targetDescriptor, _): EvaluatorFragmentParameterInfo ->
                    targetDescriptor == value
                }
            }

            val parameterPosition =
                fi.parameters.indexOfFirst(finderPredicate)
            if (parameterPosition > -1) {
                return super.referenceValueParameter(fi.methodDescriptor.valueParameters[parameterPosition])
            }

            return super.referenceValue(value)
        }
    }
}
