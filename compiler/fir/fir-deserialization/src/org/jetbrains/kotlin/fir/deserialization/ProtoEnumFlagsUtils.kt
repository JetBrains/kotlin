/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.deserialization

import org.jetbrains.kotlin.fir.types.ProjectionKind
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.serialization.deserialization.ProtoEnumFlags

@Suppress("unused")
fun ProtoEnumFlags.projection(projectionKind: ProjectionKind): ProtoBuf.Type.Argument.Projection = when (projectionKind) {
    ProjectionKind.INVARIANT -> ProtoBuf.Type.Argument.Projection.INV
    ProjectionKind.IN -> ProtoBuf.Type.Argument.Projection.IN
    ProjectionKind.OUT -> ProtoBuf.Type.Argument.Projection.OUT
    ProjectionKind.STAR -> throw AssertionError("Should not be here")
}

