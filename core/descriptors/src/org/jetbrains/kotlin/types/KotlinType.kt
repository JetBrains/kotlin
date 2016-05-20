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

import org.jetbrains.kotlin.descriptors.annotations.Annotated
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.types.checker.KotlinTypeChecker

/**
 * @see KotlinTypeChecker.isSubtypeOf
 */
interface KotlinType : Annotated {
    val constructor: TypeConstructor

    val arguments: List<TypeProjection>

    val isMarkedNullable: Boolean

    val memberScope: MemberScope

    val isError: Boolean

    override fun equals(other: Any?): Boolean

    fun <T : TypeCapability> getCapability(capabilityClass: Class<T>): T?

    val capabilities: TypeCapabilities

}

@Deprecated("Temporary marker method for refactoring")
fun KotlinType.asSimpleType(): SimpleType = this as SimpleType

interface SimpleType : KotlinType