/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.annotations

import org.jetbrains.kotlin.analysis.api.annotations.AnnotationUseSiteTargetFilter
import org.jetbrains.kotlin.analysis.api.annotations.KtAnnotationApplicationInfo
import org.jetbrains.kotlin.analysis.api.annotations.KtAnnotationApplicationWithArgumentsInfo
import org.jetbrains.kotlin.analysis.api.annotations.KtAnnotationsList
import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.name.ClassId

class KtEmptyAnnotationsList(override val token: KtLifetimeToken) : KtAnnotationsList() {
    override val annotations: List<KtAnnotationApplicationWithArgumentsInfo> get() = withValidityAssertion { emptyList() }

    override val annotationInfos: List<KtAnnotationApplicationInfo> get() = withValidityAssertion { emptyList() }

    override fun hasAnnotation(
        classId: ClassId,
        useSiteTargetFilter: AnnotationUseSiteTargetFilter,
    ): Boolean = withValidityAssertion { false }

    override fun annotationsByClassId(
        classId: ClassId,
        useSiteTargetFilter: AnnotationUseSiteTargetFilter,
    ): List<KtAnnotationApplicationWithArgumentsInfo> = withValidityAssertion { emptyList() }

    override val annotationClassIds: Collection<ClassId> get() = withValidityAssertion { emptyList() }
}
