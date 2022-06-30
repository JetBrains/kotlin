/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.scopes.receivers

import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.KotlinType

class ContextReceiver(
    override val declarationDescriptor: CallableDescriptor,
    receiverType: KotlinType,
    override val customLabelName: Name?,
    original: ReceiverValue?
) : AbstractReceiverValue(receiverType, original), ImplicitContextReceiver {
    override fun replaceType(newType: KotlinType): ReceiverValue = ContextReceiver(declarationDescriptor, newType, customLabelName, original)

    override fun toString(): String = "Cxt { $declarationDescriptor }"
}