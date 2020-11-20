/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.fir.symbols

import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.idea.frontend.api.symbols.markers.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.psi.KtCallElement

class KtFirAnnotationCall(
    override val classId: ClassId,
    override val useSiteTarget: AnnotationUseSiteTarget?,
    override val psi: KtCallElement?,
    override val arguments: List<KtNamedConstantValue>
) : KtAnnotationCall()
