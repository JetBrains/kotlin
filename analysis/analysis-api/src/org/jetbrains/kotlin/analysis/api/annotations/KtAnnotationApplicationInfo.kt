/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.annotations

import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.psi.KtCallElement

/**
 * A lightweight implementation of [KtAnnotationApplication].
 * Should be used instead of [KtAnnotationApplicationWithArgumentsInfo] where possible to avoid redundant resolve.
 *
 * Example:
 * ```
 * @Anno1(1) @Anno2
 * class Foo
 * ```
 * In this case if you don't want to process [KtAnnotationApplicationWithArgumentsInfo.arguments]
 * you can call [KtAnnotated.annotationInfos] to get all necessary information.
 *
 * @see KtAnnotated.annotationInfos
 * @see KtAnnotationApplicationWithArgumentsInfo
 */
public data class KtAnnotationApplicationInfo(
    public override val classId: ClassId?,
    public override val psi: KtCallElement?,
    public override val useSiteTarget: AnnotationUseSiteTarget?,
    public override val isCallWithArguments: Boolean,
    public override val index: Int,
) : KtAnnotationApplication
