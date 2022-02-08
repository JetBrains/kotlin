/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.scopes.receivers

import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.types.KotlinType

class ContextReceiver(
    override val declarationDescriptor: CallableDescriptor,
    receiverType: KotlinType,
    original: ReceiverValue?
) : AbstractReceiverValue(receiverType, original), ImplicitReceiver {
    override fun replaceType(newType: KotlinType): ReceiverValue = ContextReceiver(declarationDescriptor, newType, original)

    override fun toString(): String = "Cxt { $declarationDescriptor }"
}