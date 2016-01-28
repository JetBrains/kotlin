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

package org.jetbrains.kotlin.types

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.storage.StorageManager

abstract class AbstractLazyType(storageManager: StorageManager) : AbstractKotlinType(), LazyType {

    private val typeConstructor = storageManager.createLazyValue { computeTypeConstructor() }
    override fun getConstructor(): TypeConstructor = typeConstructor()

    protected abstract fun computeTypeConstructor(): TypeConstructor

    private val arguments = storageManager.createLazyValue { computeArguments() }
    override fun getArguments(): List<TypeProjection> = arguments()

    protected abstract fun computeArguments(): List<TypeProjection>

    override fun getSubstitution() = computeCustomSubstitution() ?: TypeConstructorSubstitution.create(constructor, getArguments())

    protected open fun computeCustomSubstitution(): TypeSubstitution? = getCapability<CustomSubstitutionCapability>()?.substitution

    private val memberScope = storageManager.createLazyValue { computeMemberScope() }
    override fun getMemberScope() = memberScope()

    protected open fun computeMemberScope(): MemberScope {
        val descriptor = constructor.declarationDescriptor
        return when (descriptor) {
            is TypeParameterDescriptor -> descriptor.getDefaultType().memberScope
            is ClassDescriptor -> descriptor.getMemberScope(substitution)
            else -> throw IllegalStateException("Unsupported classifier: $descriptor")
        }
    }

    override fun isMarkedNullable() = false

    override fun isError() = constructor.declarationDescriptor?.let { d -> ErrorUtils.isError(d) } ?: false

    override fun getAnnotations() = Annotations.EMPTY

    override fun toString() = when {
        !typeConstructor.isComputed() -> "[Not-computed]"
        !arguments.isComputed() ->
            if (constructor.parameters.isEmpty()) constructor.toString()
            else "$constructor<not-computed>"
        else -> super.toString()
    }
}
