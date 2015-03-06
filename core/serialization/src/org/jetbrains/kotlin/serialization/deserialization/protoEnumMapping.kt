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

import org.jetbrains.kotlin.serialization.ProtoBuf
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.serialization.ProtoBuf.TypeParameter
import org.jetbrains.kotlin.types.Variance

fun memberKind(memberKind: ProtoBuf.Callable.MemberKind) = when (memberKind) {
    ProtoBuf.Callable.MemberKind.DECLARATION -> CallableMemberDescriptor.Kind.DECLARATION
    ProtoBuf.Callable.MemberKind.FAKE_OVERRIDE -> CallableMemberDescriptor.Kind.FAKE_OVERRIDE
    ProtoBuf.Callable.MemberKind.DELEGATION -> CallableMemberDescriptor.Kind.DELEGATION
    ProtoBuf.Callable.MemberKind.SYNTHESIZED -> CallableMemberDescriptor.Kind.SYNTHESIZED
}

fun modality(modality: ProtoBuf.Modality) = when (modality) {
    ProtoBuf.Modality.FINAL -> Modality.FINAL
    ProtoBuf.Modality.OPEN -> Modality.OPEN
    ProtoBuf.Modality.ABSTRACT -> Modality.ABSTRACT
}

fun visibility(visibility: ProtoBuf.Visibility) = when (visibility) {
    ProtoBuf.Visibility.INTERNAL -> Visibilities.INTERNAL
    ProtoBuf.Visibility.PRIVATE -> Visibilities.PRIVATE
    ProtoBuf.Visibility.PRIVATE_TO_THIS -> Visibilities.PRIVATE_TO_THIS
    ProtoBuf.Visibility.PROTECTED -> Visibilities.PROTECTED
    ProtoBuf.Visibility.PUBLIC -> Visibilities.PUBLIC
    ProtoBuf.Visibility.LOCAL -> Visibilities.LOCAL
}

public fun classKind(kind: ProtoBuf.Class.Kind): ClassKind = when (kind) {
    ProtoBuf.Class.Kind.CLASS -> ClassKind.CLASS
    ProtoBuf.Class.Kind.TRAIT -> ClassKind.TRAIT
    ProtoBuf.Class.Kind.ENUM_CLASS -> ClassKind.ENUM_CLASS
    ProtoBuf.Class.Kind.ENUM_ENTRY -> ClassKind.ENUM_ENTRY
    ProtoBuf.Class.Kind.ANNOTATION_CLASS -> ClassKind.ANNOTATION_CLASS
    ProtoBuf.Class.Kind.OBJECT, ProtoBuf.Class.Kind.CLASS_OBJECT -> ClassKind.OBJECT
}

fun variance(variance: TypeParameter.Variance) = when (variance) {
    ProtoBuf.TypeParameter.Variance.IN -> Variance.IN_VARIANCE
    ProtoBuf.TypeParameter.Variance.OUT -> Variance.OUT_VARIANCE
    ProtoBuf.TypeParameter.Variance.INV -> Variance.INVARIANT
}

fun variance(variance: ProtoBuf.Type.Argument.Projection) = when (variance) {
    ProtoBuf.Type.Argument.Projection.IN -> Variance.IN_VARIANCE
    ProtoBuf.Type.Argument.Projection.OUT -> Variance.OUT_VARIANCE
    ProtoBuf.Type.Argument.Projection.INV -> Variance.INVARIANT
    else -> throw IllegalArgumentException("Only IN, OUT and INV are supported. Actual argument: $variance")
}
