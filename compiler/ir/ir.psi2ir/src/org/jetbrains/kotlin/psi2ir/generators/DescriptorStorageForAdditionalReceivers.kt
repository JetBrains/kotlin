/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi2ir.generators

import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue

class DescriptorStorageForAdditionalReceivers {
    private val fieldStorage: MutableMap<ReceiverValue, PropertyDescriptor> = mutableMapOf()
    private val variableStorage: MutableMap<KtExpression, VariableDescriptor> = mutableMapOf()

    fun put(receiverValue: ReceiverValue, descriptor: PropertyDescriptor) {
        fieldStorage[receiverValue] = descriptor
    }

    fun put(expression: KtExpression, descriptor: VariableDescriptor) {
        variableStorage[expression] = descriptor
    }

    fun getField(receiverValue: ReceiverValue) =
        fieldStorage[receiverValue] ?: error("No field descriptor for receiver value $receiverValue")

    fun getVariable(expression: KtExpression) = variableStorage[expression] ?: error("No variable descriptor for receiver $expression")
}