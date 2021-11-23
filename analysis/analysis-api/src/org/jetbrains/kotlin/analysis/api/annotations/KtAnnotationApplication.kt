/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.annotations

import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtCallElement

public class KtAnnotationApplication(
    public val classId: ClassId?,
    public val psi: KtAnnotationEntry?,
    public val useSiteTarget: AnnotationUseSiteTarget?,
    public val arguments: List<KtNamedAnnotationValue>,
)