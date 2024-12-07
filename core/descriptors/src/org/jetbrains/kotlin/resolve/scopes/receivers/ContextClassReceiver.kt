/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.scopes.receivers

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.KotlinType

class ContextClassReceiver(
    val classDescriptor: ClassDescriptor,
    receiverType: KotlinType,
    override val customLabelName: Name?,
    original: ReceiverValue?
): AbstractReceiverValue(receiverType, original), ImplicitContextReceiver {
    override val declarationDescriptor: DeclarationDescriptor
        get() = classDescriptor

    override fun replaceType(newType: KotlinType): ReceiverValue = ContextClassReceiver(classDescriptor, newType, customLabelName, original)

    override fun toString(): String = "$type: Ctx { $classDescriptor }"
}