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

package org.jetbrains.kotlin.idea.core.extension

import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.extensions.ProjectExtensionDescriptor
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.types.KotlinType

interface KotlinIndicesHelperExtension {
    companion object : ProjectExtensionDescriptor<KotlinIndicesHelperExtension>(
        "org.jetbrains.kotlin.kotlinIndicesHelperExtension", KotlinIndicesHelperExtension::class.java
    )

    @JvmDefault
    fun appendExtensionCallables(
        consumer: MutableList<in CallableDescriptor>,
        moduleDescriptor: ModuleDescriptor,
        receiverTypes: Collection<KotlinType>,
        nameFilter: (String) -> Boolean,
        lookupLocation: LookupLocation
    ) {
        appendExtensionCallables(consumer, moduleDescriptor, receiverTypes, nameFilter)
    }

    @Deprecated(
        "Override the appendExtensionCallables() with the 'lookupLocation' parameter instead. " +
                "This method can then throw an exception. " +
                "See 'DebuggerFieldKotlinIndicesHelperExtension' as an example."
    )
    fun appendExtensionCallables(
        consumer: MutableList<in CallableDescriptor>,
        moduleDescriptor: ModuleDescriptor,
        receiverTypes: Collection<KotlinType>,
        nameFilter: (String) -> Boolean
    )
}