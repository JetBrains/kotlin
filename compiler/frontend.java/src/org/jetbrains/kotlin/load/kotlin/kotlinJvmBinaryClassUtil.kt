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

package org.jetbrains.kotlin.load.kotlin

import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.load.java.lazy.descriptors.LazyJavaPackageFragment
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedCallableMemberDescriptor
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedClassDescriptor
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

fun CallableDescriptor.getContainingKotlinJvmBinaryClass(): KotlinJvmBinaryClass? {
    if (this !is DeserializedCallableMemberDescriptor) return null

    val container = containingDeclaration

    return when (container) {
        is DeserializedClassDescriptor ->
            container.source.safeAs<KotlinJvmBinarySourceElement>()?.binaryClass
        is LazyJavaPackageFragment ->
            container.source.safeAs<KotlinJvmBinaryPackageSourceElement>()?.getRepresentativeBinaryClass()
        else -> null
    }
}
