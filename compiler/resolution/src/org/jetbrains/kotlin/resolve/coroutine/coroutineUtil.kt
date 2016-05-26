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

package org.jetbrains.kotlin.resolve.coroutine

import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.resolve.scopes.receivers.ExtensionReceiver
import org.jetbrains.kotlin.types.KotlinType

// Returns suspension function as it's visible within coroutines:
// E.g. `fun <V> await(f: CompletableFuture<V>): V` instead of `fun <V> await(f: CompletableFuture<V>, machine: Continuation<V>): Unit`
fun SimpleFunctionDescriptor.createCoroutineSuspensionFunctionView(): SimpleFunctionDescriptor? {
    if (!isSuspend) return null
    val returnType = valueParameters.lastOrNull()?.returnType?.arguments?.getOrNull(0)?.type ?: return null

    val newOriginal =
            if (original !== this)
                original.createCoroutineSuspensionFunctionView()
            else null

    return newCopyBuilder().apply {
        setReturnType(returnType)
        setOriginal(newOriginal)
        setValueParameters(valueParameters.subList(0, valueParameters.size - 1))
        setSignatureChange()
    }.build()!!
}

class CoroutineReceiverValue(callableDescriptor: CallableDescriptor, receiverType: KotlinType) : ExtensionReceiver(callableDescriptor, receiverType)
