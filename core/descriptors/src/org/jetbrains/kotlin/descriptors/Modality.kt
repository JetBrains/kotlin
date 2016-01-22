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

import org.jetbrains.kotlin.resolve.DescriptorUtils

// For sealed classes, isOverridable is false but isOverridableByMembers is true
enum class Modality {
    // THE ORDER OF ENTRIES MATTERS HERE
    FINAL,
    // NB: class can be sealed but not function or property
    SEALED,
    OPEN,
    ABSTRACT;

    companion object {

        // NB: never returns SEALED
        fun convertFromFlags(abstract: Boolean, open: Boolean): Modality {
            if (abstract) return ABSTRACT
            if (open) return OPEN
            return FINAL
        }
    }
}

val CallableMemberDescriptor.isOverridable: Boolean
    get() = modality != Modality.FINAL && (containingDeclaration as? ClassDescriptor)?.isFinalClass != true

val CallableMemberDescriptor.isOverridableOrOverrides: Boolean
    get() = isOverridable || DescriptorUtils.isOverride(this)

val ClassDescriptor.isFinalClass: Boolean
    get() = modality == Modality.FINAL && kind != ClassKind.ENUM_CLASS

val ClassDescriptor.isFinalOrEnum: Boolean
    get() = modality == Modality.FINAL
