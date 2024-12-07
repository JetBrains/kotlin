/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.serialization.deserialization

import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.deserialization.NameResolver
import org.jetbrains.kotlin.protobuf.MessageLite

// The MessageLite instance everywhere should be Constructor, Function or Property
// TODO: simplify this interface
interface AnnotationLoader<out A : Any> {
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

    fun loadAnnotation(proto: ProtoBuf.Annotation, nameResolver: NameResolver): A
}