/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.annotations

import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.psi.KtCallElement

/**
 * Info of annotation to some declaration or type.
 *
 * Some examples:
 * - For declarations: `@Deprecated("Should not be used") fun foo(){}`
 * - For types: `fun foo(x: List<@A Int>){}`
 */
public data class KtAnnotationApplicationInfo(
    public override val classId: ClassId?,
    public override val psi: KtCallElement?,
    public override val useSiteTarget: AnnotationUseSiteTarget?,
    public override val isCallWithArguments: Boolean,
    public override val index: Int,
) : KtAnnotationApplication
