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
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.resolve.scopes.MemberScope

open class KotlinTypeImpl
private constructor(
        override val annotations: Annotations,
        final override val constructor: TypeConstructor,
        override val isMarkedNullable: Boolean,
        override val arguments: List<TypeProjection>,
        final override val memberScope: MemberScope
) : AbstractKotlinType() {

    companion object {
        @JvmStatic fun create(annotations: Annotations,
                              constructor: TypeConstructor,
                              nullable: Boolean,
                              arguments: List<TypeProjection>,
                              memberScope: MemberScope): KotlinTypeImpl

                = KotlinTypeImpl(annotations, constructor, nullable, arguments, memberScope)

        @JvmStatic fun create(annotations: Annotations,
                              constructor: TypeConstructor,
                              nullable: Boolean,
                              arguments: List<TypeProjection>,
                              memberScope: MemberScope,
                              capabilities: TypeCapabilities
        ): KotlinTypeImpl {
            if (capabilities !== TypeCapabilities.NONE) {
                return WithCapabilities(annotations, constructor, nullable, arguments, memberScope, capabilities)
            }
            return KotlinTypeImpl(annotations, constructor, nullable, arguments, memberScope)
        }

        @JvmStatic fun create(annotations: Annotations,
                              descriptor: ClassDescriptor,
                              nullable: Boolean,
                              arguments: List<TypeProjection>): KotlinTypeImpl

                = KotlinTypeImpl(
                    annotations, descriptor.typeConstructor, nullable, arguments, descriptor.getMemberScope(arguments)
                )
    }

    private class WithCapabilities(
            annotations: Annotations,
            constructor: TypeConstructor,
            nullable: Boolean,
            arguments: List<TypeProjection>,
            memberScope: MemberScope,
            override val capabilities: TypeCapabilities
    ) : KotlinTypeImpl(annotations, constructor, nullable, arguments, memberScope)

    init {
        if (memberScope is ErrorUtils.ErrorScope) {
            throw IllegalStateException("JetTypeImpl should not be created for error type: $memberScope\n$constructor")
        }
    }

    override val isError: Boolean get() = false
}
