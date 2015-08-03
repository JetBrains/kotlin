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
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.resolve.scopes.JetScope
import kotlin.platform.platformStatic

public open class JetTypeImpl
private constructor(
        private val annotations: Annotations,
        private val constructor: TypeConstructor,
        private val nullable: Boolean,
        private val arguments: List<TypeProjection>,
        private val substitution: TypeSubstitution?,
        private val memberScope: JetScope
) : AbstractJetType() {

    companion object {
        @platformStatic
        public fun create(annotations: Annotations,
                          constructor: TypeConstructor,
                          nullable: Boolean,
                          arguments: List<TypeProjection>,
                          memberScope: JetScope): JetTypeImpl

                = JetTypeImpl(annotations, constructor, nullable, arguments, null, memberScope)

        @platformStatic
        public fun create(annotations: Annotations,
                          constructor: TypeConstructor,
                          nullable: Boolean,
                          arguments: List<TypeProjection>,
                          substitution: TypeSubstitution,
                          memberScope: JetScope,
                          capabilities: TypeCapabilities
        ): JetTypeImpl {
            if (capabilities !== TypeCapabilities.NONE) {
                return WithCapabilities(annotations, constructor, nullable, arguments, substitution, memberScope, capabilities)
            }
            return JetTypeImpl(annotations, constructor, nullable, arguments, substitution, memberScope)
        }

        @platformStatic
        public fun create(annotations: Annotations,
                          descriptor: ClassDescriptor,
                          nullable: Boolean,
                          arguments: List<TypeProjection>): JetTypeImpl

                = JetTypeImpl(
                    annotations, descriptor.typeConstructor, nullable, arguments, null, descriptor.getMemberScope(arguments)
                )
    }

    private class WithCapabilities(
            annotations: Annotations,
            constructor: TypeConstructor,
            nullable: Boolean,
            arguments: List<TypeProjection>,
            substitution: TypeSubstitution?,
            memberScope: JetScope,
            private val typeCapabilities: TypeCapabilities
    ) : JetTypeImpl(annotations, constructor, nullable, arguments, substitution, memberScope) {
        override fun getCapabilities(): TypeCapabilities = typeCapabilities
    }

    init {
        if (memberScope is ErrorUtils.ErrorScope) {
            throw IllegalStateException("JetTypeImpl should not be created for error type: $memberScope\n$constructor")
        }
    }

    override fun getAnnotations() = annotations

    override fun getSubstitution(): TypeSubstitution {
        if (substitution == null) {
            return IndexedParametersSubstitution(getConstructor(), getArguments())
        }
        return substitution
    }

    override fun getConstructor() = constructor

    override fun getArguments() = arguments

    override fun isMarkedNullable() = nullable

    override fun getMemberScope() = memberScope

    override fun isError() = false
}
