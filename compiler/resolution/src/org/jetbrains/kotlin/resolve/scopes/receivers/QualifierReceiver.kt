/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.resolve.scopes.receivers

import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.checker.*

// this receiver used only for resolution. see subtypes
interface DetailedReceiver

class ReceiverValueWithSmartCastInfo(
    val receiverValue: ReceiverValue,
    /*
     * It doesn't include receiver.type and is used only to special marking such types (e.g. for IDE green highlighting)
     * but not to construct the resulting type
     */
    val typesFromSmartCasts: Set<KotlinType>,
    val isStable: Boolean,
    originalBaseType: KotlinType = receiverValue.type
) : DetailedReceiver {
    // It's used to construct the resulting type
    val allOriginalTypes = typesFromSmartCasts + originalBaseType

    fun hasTypesFromSmartCasts() = typesFromSmartCasts.isNotEmpty()

    override fun toString() = receiverValue.toString()
}

interface QualifierReceiver : Receiver, DetailedReceiver {
    val descriptor: DeclarationDescriptor

    val staticScope: MemberScope

    val classValueReceiver: ReceiverValue?

    // for qualifiers smart cast is impossible
    val classValueReceiverWithSmartCastInfo: ReceiverValueWithSmartCastInfo?
        get() = classValueReceiver?.let { ReceiverValueWithSmartCastInfo(it, emptySet(), true) }
}

fun ReceiverValueWithSmartCastInfo.prepareReceiverRegardingCaptureTypes(): ReceiverValueWithSmartCastInfo {
    val preparedBaseType = prepareArgumentTypeRegardingCaptureTypes(receiverValue.type.unwrap()) ?: return this

    return ReceiverValueWithSmartCastInfo(receiverValue.replaceType(preparedBaseType), typesFromSmartCasts, isStable, receiverValue.type)
}
