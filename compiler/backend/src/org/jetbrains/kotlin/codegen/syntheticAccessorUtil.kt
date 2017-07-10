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

package org.jetbrains.kotlin.codegen

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.resolve.DescriptorUtils

enum class FieldAccessorKind(val suffix: String) {
    NORMAL("p"),
    IN_CLASS_COMPANION("cp"),
    FIELD_FROM_LOCAL("lp");

    override fun toString() = suffix
}

private fun CallableMemberDescriptor.getJvmName() =
        DescriptorUtils.getJvmName(this) ?: name.asString()

fun getAccessorNameSuffix(descriptor: CallableMemberDescriptor, superCallDescriptor: ClassDescriptor?,
                          accessorKind: FieldAccessorKind): String {
    val suffix = when (descriptor) {
        is ConstructorDescriptor ->
            return "will be ignored"
        is SimpleFunctionDescriptor ->
            descriptor.getJvmName()
        is PropertyDescriptor ->
            descriptor.getJvmName() + "$" + accessorKind
        else ->
            throw UnsupportedOperationException("Do not know how to create accessor for descriptor " + descriptor)
    }

    return if (superCallDescriptor == null) suffix else "$suffix\$s${superCallDescriptor.name.asString().hashCode()}"
}