/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.annotations

import org.jetbrains.kotlin.analysis.api.annotations.KtAnnotationApplication
import org.jetbrains.kotlin.analysis.api.annotations.KtAnnotationsList
import org.jetbrains.kotlin.analysis.api.tokens.ValidityToken
import org.jetbrains.kotlin.analysis.api.withValidityAssertion
import org.jetbrains.kotlin.name.ClassId

class KtEmptyAnnotationsList(override val token: ValidityToken) : KtAnnotationsList() {
    override val annotations: List<KtAnnotationApplication>
        get() = withValidityAssertion { emptyList() }

    override fun containsAnnotation(classId: ClassId): Boolean = withValidityAssertion { false }

    override fun annotationsByClassId(classId: ClassId): List<KtAnnotationApplication> =
        withValidityAssertion { emptyList() }

    override val annotationClassIds: Collection<ClassId>
        get() = withValidityAssertion { emptyList() }
}