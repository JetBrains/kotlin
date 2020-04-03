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

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.ProtoBuf.TypeParameter
import org.jetbrains.kotlin.types.Variance

object ProtoEnumFlags {
    fun memberKind(memberKind: ProtoBuf.MemberKind?) = when (memberKind) {
        ProtoBuf.MemberKind.DECLARATION -> CallableMemberDescriptor.Kind.DECLARATION
        ProtoBuf.MemberKind.FAKE_OVERRIDE -> CallableMemberDescriptor.Kind.FAKE_OVERRIDE
        ProtoBuf.MemberKind.DELEGATION -> CallableMemberDescriptor.Kind.DELEGATION
        ProtoBuf.MemberKind.SYNTHESIZED -> CallableMemberDescriptor.Kind.SYNTHESIZED
        else -> CallableMemberDescriptor.Kind.DECLARATION
    }

    fun memberKind(kind: CallableMemberDescriptor.Kind): ProtoBuf.MemberKind = when (kind) {
        CallableMemberDescriptor.Kind.DECLARATION -> ProtoBuf.MemberKind.DECLARATION
        CallableMemberDescriptor.Kind.FAKE_OVERRIDE -> ProtoBuf.MemberKind.FAKE_OVERRIDE
        CallableMemberDescriptor.Kind.DELEGATION -> ProtoBuf.MemberKind.DELEGATION
        CallableMemberDescriptor.Kind.SYNTHESIZED -> ProtoBuf.MemberKind.SYNTHESIZED
    }

    fun modality(modality: ProtoBuf.Modality?) = when (modality) {
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

    fun visibility(visibility: ProtoBuf.Visibility?) = when (visibility) {
        ProtoBuf.Visibility.INTERNAL -> Visibilities.INTERNAL
        ProtoBuf.Visibility.PRIVATE -> Visibilities.PRIVATE
        ProtoBuf.Visibility.PRIVATE_TO_THIS -> Visibilities.PRIVATE_TO_THIS
        ProtoBuf.Visibility.PROTECTED -> Visibilities.PROTECTED
        ProtoBuf.Visibility.PUBLIC -> Visibilities.PUBLIC
        ProtoBuf.Visibility.LOCAL -> Visibilities.LOCAL
        else -> Visibilities.PRIVATE
    }

    fun visibility(visibility: Visibility): ProtoBuf.Visibility = when (visibility) {
        Visibilities.INTERNAL -> ProtoBuf.Visibility.INTERNAL
        Visibilities.PUBLIC -> ProtoBuf.Visibility.PUBLIC
        Visibilities.PRIVATE -> ProtoBuf.Visibility.PRIVATE
        Visibilities.PRIVATE_TO_THIS -> ProtoBuf.Visibility.PRIVATE_TO_THIS
        Visibilities.PROTECTED -> ProtoBuf.Visibility.PROTECTED
        Visibilities.LOCAL -> ProtoBuf.Visibility.LOCAL
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

    fun variance(variance: TypeParameter.Variance) = when (variance) {
        ProtoBuf.TypeParameter.Variance.IN -> Variance.IN_VARIANCE
        ProtoBuf.TypeParameter.Variance.OUT -> Variance.OUT_VARIANCE
        ProtoBuf.TypeParameter.Variance.INV -> Variance.INVARIANT
    }

    fun variance(projection: ProtoBuf.Type.Argument.Projection) = when (projection) {
        ProtoBuf.Type.Argument.Projection.IN -> Variance.IN_VARIANCE
        ProtoBuf.Type.Argument.Projection.OUT -> Variance.OUT_VARIANCE
        ProtoBuf.Type.Argument.Projection.INV -> Variance.INVARIANT
        ProtoBuf.Type.Argument.Projection.STAR ->
            throw IllegalArgumentException("Only IN, OUT and INV are supported. Actual argument: $projection")
    }

    fun variance(variance: Variance) = when (variance) {
        Variance.IN_VARIANCE -> TypeParameter.Variance.IN
        Variance.OUT_VARIANCE -> TypeParameter.Variance.OUT
        Variance.INVARIANT -> TypeParameter.Variance.INV
    }
}
