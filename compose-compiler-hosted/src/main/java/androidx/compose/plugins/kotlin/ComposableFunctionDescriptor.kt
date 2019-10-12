/*
 * Copyright 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.plugins.kotlin

import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.types.TypeSubstitutor

class ComposableFunctionDescriptor(
    val underlyingDescriptor: FunctionDescriptor,
    val composerCall: ResolvedCall<*>,
    val composerMetadata: ComposerMetadata
) : FunctionDescriptor by underlyingDescriptor {
    override fun substitute(substitutor: TypeSubstitutor): FunctionDescriptor? {
        return underlyingDescriptor.substitute(substitutor)?.let {
            ComposableFunctionDescriptor(
                underlyingDescriptor = it,
                composerCall = composerCall,
                composerMetadata = composerMetadata
            )
        }
    }
}
