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

package org.jetbrains.kotlin.types

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.storage.getValue

abstract class AbstractLazyType(storageManager: StorageManager) : AbstractKotlinType(), SimpleType, LazyType {

    private val typeConstructor = storageManager.createLazyValue { computeTypeConstructor() }
    override val constructor by typeConstructor

    protected abstract fun computeTypeConstructor(): TypeConstructor

    private val _arguments = storageManager.createLazyValue { computeArguments() }
    override val arguments by _arguments

    protected abstract fun computeArguments(): List<TypeProjection>

    override val memberScope by storageManager.createLazyValue { computeMemberScope() }

    fun computeMemberScope(): MemberScope {
        val descriptor = constructor.declarationDescriptor
        return when (descriptor) {
            is TypeParameterDescriptor -> descriptor.getDefaultType().memberScope
            is ClassDescriptor ->  descriptor.getMemberScope(TypeConstructorSubstitution.create(constructor, arguments))
            else -> throw IllegalStateException("Unsupported classifier: $descriptor")
        }
    }

    override val isMarkedNullable: Boolean get() = false

    override val isError: Boolean get() = constructor.declarationDescriptor?.let { d -> ErrorUtils.isError(d) } ?: false

    override val annotations: Annotations get() = Annotations.EMPTY

    override fun toString() = when {
        !typeConstructor.isComputed() -> "[Not-computed]"
        !_arguments.isComputed() ->
            if (constructor.parameters.isEmpty()) constructor.toString()
            else "$constructor<not-computed>"
        else -> super.toString()
    }
}
