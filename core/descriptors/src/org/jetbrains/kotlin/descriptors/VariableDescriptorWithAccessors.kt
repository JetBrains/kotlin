/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.descriptors

interface VariableDescriptorWithAccessors : VariableDescriptor {
    val getter: VariableAccessorDescriptor?

    val setter: VariableAccessorDescriptor?

    /**
     * Please be careful with this method. Depending on the fact that a property is delegated may be dangerous in the compiler.
     * Whether or not a property is delegated is neither the API or the ABI of that property, and one should be able to recompile a library
     * in a way that makes some non-delegated properties delegated or vice versa, without any problems at compilation time or at runtime.
     *
     * This flag is needed for reflection however, that's why it's serialized to metadata and is exposed in this interface.
     */
    @Deprecated("Do not call this method in the compiler front-end.")
    val isDelegated: Boolean
}
