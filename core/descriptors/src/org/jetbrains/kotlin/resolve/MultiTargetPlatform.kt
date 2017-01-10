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

package org.jetbrains.kotlin.resolve

import org.jetbrains.kotlin.descriptors.MemberDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.resolve.descriptorUtil.module

sealed class MultiTargetPlatform : Comparable<MultiTargetPlatform> {
    object Common : MultiTargetPlatform() {
        override fun compareTo(other: MultiTargetPlatform): Int =
                if (other is Common) 0 else -1
    }

    data class Specific(val platform: String) : MultiTargetPlatform() {
        override fun compareTo(other: MultiTargetPlatform): Int =
                when (other) {
                    is Common -> 1
                    is Specific -> platform.compareTo(other.platform)
                }
    }

    companion object {
        @JvmField
        val CAPABILITY = ModuleDescriptor.Capability<MultiTargetPlatform>("MULTI_TARGET_PLATFORM")
    }
}

fun ModuleDescriptor.getMultiTargetPlatform(): MultiTargetPlatform? =
        module.getCapability(MultiTargetPlatform.CAPABILITY)

fun MemberDescriptor.getMultiTargetPlatform(): String? =
        (module.getMultiTargetPlatform() as? MultiTargetPlatform.Specific)?.platform
