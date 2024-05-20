/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.annotations

import org.jetbrains.kotlin.analysis.api.annotations.AnnotationUseSiteTargetFilter
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationApplicationInfo
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationApplicationWithArgumentsInfo
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationsList
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.name.ClassId

class KaEmptyAnnotationsList(override val token: KaLifetimeToken) : KaAnnotationsList() {
    override val annotations: List<KaAnnotationApplicationWithArgumentsInfo> get() = withValidityAssertion { emptyList() }

    override val annotationInfos: List<KaAnnotationApplicationInfo> get() = withValidityAssertion { emptyList() }

    override fun hasAnnotation(
        classId: ClassId,
        useSiteTargetFilter: AnnotationUseSiteTargetFilter,
    ): Boolean = withValidityAssertion { false }

    override fun annotationsByClassId(
        classId: ClassId,
        useSiteTargetFilter: AnnotationUseSiteTargetFilter,
    ): List<KaAnnotationApplicationWithArgumentsInfo> = withValidityAssertion { emptyList() }

    override val annotationClassIds: Collection<ClassId> get() = withValidityAssertion { emptyList() }
}
