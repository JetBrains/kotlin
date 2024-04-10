/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.renderer.base.annotations

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.annotations.KtAnnotated
import org.jetbrains.kotlin.analysis.api.annotations.KtAnnotationApplication
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.load.java.NULLABILITY_ANNOTATIONS

public interface KtRendererAnnotationsFilter {
    public fun filter(analysisSession: KtAnalysisSession, annotation: KtAnnotationApplication, owner: KtAnnotated): Boolean

    public infix fun and(other: KtRendererAnnotationsFilter): KtRendererAnnotationsFilter =
        KtRendererAnnotationsFilter filter@{ annotation, owner ->
            val analysisSession = this@filter
            filter(analysisSession, annotation, owner) && other.filter(analysisSession, annotation, owner)
        }

    public infix fun or(other: KtRendererAnnotationsFilter): KtRendererAnnotationsFilter =
        KtRendererAnnotationsFilter filter@{ annotation, owner ->
            val analysisSession = this@filter
            filter(analysisSession, annotation, owner) || other.filter(analysisSession, annotation, owner)
        }

    public object ALL : KtRendererAnnotationsFilter {
        override fun filter(analysisSession: KtAnalysisSession, annotation: KtAnnotationApplication, owner: KtAnnotated): Boolean {
            return true
        }
    }

    public object NO_NULLABILITY : KtRendererAnnotationsFilter {
        override fun filter(analysisSession: KtAnalysisSession, annotation: KtAnnotationApplication, owner: KtAnnotated): Boolean {
            return annotation.classId?.asSingleFqName() !in NULLABILITY_ANNOTATIONS
        }
    }

    public object NO_PARAMETER_NAME : KtRendererAnnotationsFilter {
        override fun filter(analysisSession: KtAnalysisSession, annotation: KtAnnotationApplication, owner: KtAnnotated): Boolean {
            return annotation.classId?.asSingleFqName() != StandardNames.FqNames.parameterName
        }
    }


    public object NONE : KtRendererAnnotationsFilter {
        override fun filter(analysisSession: KtAnalysisSession, annotation: KtAnnotationApplication, owner: KtAnnotated): Boolean {
            return false
        }
    }

    public companion object {
        public operator fun invoke(
            predicate: KtAnalysisSession.(annotation: KtAnnotationApplication, owner: KtAnnotated) -> Boolean
        ): KtRendererAnnotationsFilter = object : KtRendererAnnotationsFilter {
            override fun filter(analysisSession: KtAnalysisSession, annotation: KtAnnotationApplication, owner: KtAnnotated): Boolean {
                return predicate(analysisSession, annotation, owner)
            }
        }
    }
}
