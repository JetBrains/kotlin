/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.backend.common.descriptors

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.descriptors.explicitParameters as _explicitParameters

val String.synthesizedName: Name get() = Name.identifier(this.synthesizedString)

val String.synthesizedString: String get() = "\$$this"

val CallableDescriptor.isSuspend: Boolean
    get() = this is FunctionDescriptor && isSuspend

/**
 * @return naturally-ordered list of all parameters available inside the function body.
 */
// Used in Kotlin/Native
@Suppress("unused")
val CallableDescriptor.allParameters: List<ParameterDescriptor>
    get() = if (this is ConstructorDescriptor) {
        listOf(this.constructedClass.thisAsReceiverParameter) + _explicitParameters
    } else {
        _explicitParameters
    }
