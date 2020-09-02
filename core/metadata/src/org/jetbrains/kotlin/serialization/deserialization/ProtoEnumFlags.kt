/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.serialization.deserialization

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.ProtoBuf.TypeParameter
import org.jetbrains.kotlin.types.Variance

object ProtoEnumFlags {
    fun modality(modality: ProtoBuf.Modality?): Modality = when (modality) {
        ProtoBuf.Modality.FINAL -> Modality.FINAL
        ProtoBuf.Modality.OPEN -> Modality.OPEN
        ProtoBuf.Modality.ABSTRACT -> Modality.ABSTRACT
        ProtoBuf.Modality.SEALED -> Modality.SEALED
        else -> Modality.FINAL
    }

    fun modality(modality: Modality): ProtoBuf.Modality = when (modality) {
        Modality.FINAL -> ProtoBuf.Modality.FINAL
        Modality.OPEN -> ProtoBuf.Modality.OPEN
        Modality.ABSTRACT -> ProtoBuf.Modality.ABSTRACT
        Modality.SEALED -> ProtoBuf.Modality.SEALED
    }

    fun visibility(visibility: ProtoBuf.Visibility?): Visibility = when (visibility) {
        ProtoBuf.Visibility.INTERNAL -> Visibilities.Internal
        ProtoBuf.Visibility.PRIVATE -> Visibilities.Private
        ProtoBuf.Visibility.PRIVATE_TO_THIS -> Visibilities.PrivateToThis
        ProtoBuf.Visibility.PROTECTED -> Visibilities.Protected
        ProtoBuf.Visibility.PUBLIC -> Visibilities.Public
        ProtoBuf.Visibility.LOCAL -> Visibilities.Local
        else -> Visibilities.Private
    }

    fun visibility(visibility: Visibility): ProtoBuf.Visibility = when (visibility) {
        Visibilities.Internal -> ProtoBuf.Visibility.INTERNAL
        Visibilities.Public -> ProtoBuf.Visibility.PUBLIC
        Visibilities.Private -> ProtoBuf.Visibility.PRIVATE
        Visibilities.PrivateToThis -> ProtoBuf.Visibility.PRIVATE_TO_THIS
        Visibilities.Protected -> ProtoBuf.Visibility.PROTECTED
        Visibilities.Local -> ProtoBuf.Visibility.LOCAL
        else -> throw IllegalArgumentException("Unknown visibility: $visibility")
    }

    fun classKind(kind: ProtoBuf.Class.Kind?): ClassKind = when (kind) {
        ProtoBuf.Class.Kind.CLASS -> ClassKind.CLASS
        ProtoBuf.Class.Kind.INTERFACE -> ClassKind.INTERFACE
        ProtoBuf.Class.Kind.ENUM_CLASS -> ClassKind.ENUM_CLASS
        ProtoBuf.Class.Kind.ENUM_ENTRY -> ClassKind.ENUM_ENTRY
        ProtoBuf.Class.Kind.ANNOTATION_CLASS -> ClassKind.ANNOTATION_CLASS
        ProtoBuf.Class.Kind.OBJECT, ProtoBuf.Class.Kind.COMPANION_OBJECT -> ClassKind.OBJECT
        else -> ClassKind.CLASS
    }

    fun classKind(kind: ClassKind, isCompanionObject: Boolean): ProtoBuf.Class.Kind {
        if (isCompanionObject) return ProtoBuf.Class.Kind.COMPANION_OBJECT

        return when (kind) {
            ClassKind.CLASS -> ProtoBuf.Class.Kind.CLASS
            ClassKind.INTERFACE -> ProtoBuf.Class.Kind.INTERFACE
            ClassKind.ENUM_CLASS -> ProtoBuf.Class.Kind.ENUM_CLASS
            ClassKind.ENUM_ENTRY -> ProtoBuf.Class.Kind.ENUM_ENTRY
            ClassKind.ANNOTATION_CLASS -> ProtoBuf.Class.Kind.ANNOTATION_CLASS
            ClassKind.OBJECT -> ProtoBuf.Class.Kind.OBJECT
        }
    }

    fun variance(variance: TypeParameter.Variance): Variance = when (variance) {
        ProtoBuf.TypeParameter.Variance.IN -> Variance.IN_VARIANCE
        ProtoBuf.TypeParameter.Variance.OUT -> Variance.OUT_VARIANCE
        ProtoBuf.TypeParameter.Variance.INV -> Variance.INVARIANT
    }

    fun variance(projection: ProtoBuf.Type.Argument.Projection): Variance = when (projection) {
        ProtoBuf.Type.Argument.Projection.IN -> Variance.IN_VARIANCE
        ProtoBuf.Type.Argument.Projection.OUT -> Variance.OUT_VARIANCE
        ProtoBuf.Type.Argument.Projection.INV -> Variance.INVARIANT
        ProtoBuf.Type.Argument.Projection.STAR ->
            throw IllegalArgumentException("Only IN, OUT and INV are supported. Actual argument: $projection")
    }

    fun variance(variance: Variance): TypeParameter.Variance = when (variance) {
        Variance.IN_VARIANCE -> TypeParameter.Variance.IN
        Variance.OUT_VARIANCE -> TypeParameter.Variance.OUT
        Variance.INVARIANT -> TypeParameter.Variance.INV
    }
}
