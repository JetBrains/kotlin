/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.types

import org.jetbrains.kotlin.fir.UnambiguousFqName

sealed class ConeKotlinTypeProjection(val kind: ProjectionKind)

enum class ProjectionKind {
    STAR, IN, OUT, INVARIANT
}


object StarProjection : ConeKotlinTypeProjection(ProjectionKind.STAR)

abstract class ConeKotlinTypeProjectionIn : ConeKotlinTypeProjection(ProjectionKind.IN) {
    abstract val type: ConeKotlinType
}

abstract class ConeKotlinTypeProjectionOut : ConeKotlinTypeProjection(ProjectionKind.OUT) {
    abstract val type: ConeKotlinType
}

abstract class ConeKotlinType : ConeKotlinTypeProjection(ProjectionKind.INVARIANT) {
    abstract val typeArguments: List<ConeKotlinTypeProjection>
}

abstract class ConeClassType : ConeKotlinType() {
    abstract val fqName: UnambiguousFqName
}