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

package org.jetbrains.kotlin.serialization.deserialization

import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.deserialization.NameResolver
import org.jetbrains.kotlin.protobuf.MessageLite
import org.jetbrains.kotlin.types.KotlinType

// The MessageLite instance everywhere should be Constructor, Function or Property
// TODO: simplify this interface
interface AnnotationAndConstantLoader<out A : Any, out C : Any> {
    fun loadClassAnnotations(
        container: ProtoContainer.Class
    ): List<A>

    fun loadCallableAnnotations(
        container: ProtoContainer,
        proto: MessageLite,
        kind: AnnotatedCallableKind
    ): List<A>

    fun loadPropertyBackingFieldAnnotations(
        container: ProtoContainer,
        proto: ProtoBuf.Property
    ): List<A>

    fun loadPropertyDelegateFieldAnnotations(
        container: ProtoContainer,
        proto: ProtoBuf.Property
    ): List<A>

    fun loadEnumEntryAnnotations(
        container: ProtoContainer,
        proto: ProtoBuf.EnumEntry
    ): List<A>

    fun loadValueParameterAnnotations(
        container: ProtoContainer,
        callableProto: MessageLite,
        kind: AnnotatedCallableKind,
        parameterIndex: Int,
        proto: ProtoBuf.ValueParameter
    ): List<A>

    fun loadExtensionReceiverParameterAnnotations(
        container: ProtoContainer,
        proto: MessageLite,
        kind: AnnotatedCallableKind
    ): List<A>

    fun loadTypeAnnotations(
        proto: ProtoBuf.Type,
        nameResolver: NameResolver
    ): List<A>

    fun loadTypeParameterAnnotations(
        proto: ProtoBuf.TypeParameter,
        nameResolver: NameResolver
    ): List<A>

    fun loadPropertyConstant(
        container: ProtoContainer,
        proto: ProtoBuf.Property,
        expectedType: KotlinType
    ): C?
}
