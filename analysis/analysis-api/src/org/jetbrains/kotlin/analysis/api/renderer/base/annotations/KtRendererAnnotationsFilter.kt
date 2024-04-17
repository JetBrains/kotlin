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
    context(KtAnalysisSession)
    public fun filter(annotation: KtAnnotationApplication, owner: KtAnnotated): Boolean

    public infix fun and(other: KtRendererAnnotationsFilter): KtRendererAnnotationsFilter =
        KtRendererAnnotationsFilter { annotation, owner ->
            filter(annotation, owner) && other.filter(annotation, owner)
        }

    public infix fun or(other: KtRendererAnnotationsFilter): KtRendererAnnotationsFilter =
        KtRendererAnnotationsFilter { annotation, owner ->
            filter(annotation, owner) || other.filter(annotation, owner)
        }

    public object ALL : KtRendererAnnotationsFilter {
        context(KtAnalysisSession)
        override fun filter(annotation: KtAnnotationApplication, owner: KtAnnotated): Boolean {
            return true
        }
    }

    public object NO_NULLABILITY : KtRendererAnnotationsFilter {
        context(KtAnalysisSession)
        override fun filter(annotation: KtAnnotationApplication, owner: KtAnnotated): Boolean {
            return annotation.classId?.asSingleFqName() !in NULLABILITY_ANNOTATIONS
        }
    }

    public object NO_PARAMETER_NAME : KtRendererAnnotationsFilter {
        context(KtAnalysisSession)
        override fun filter(annotation: KtAnnotationApplication, owner: KtAnnotated): Boolean {
            return annotation.classId?.asSingleFqName() != StandardNames.FqNames.parameterName
        }
    }


    public object NONE : KtRendererAnnotationsFilter {
        context(KtAnalysisSession)
        override fun filter(annotation: KtAnnotationApplication, owner: KtAnnotated): Boolean {
            return false
        }
    }

    public companion object {
        public operator fun invoke(
            predicate: context(KtAnalysisSession) (annotation: KtAnnotationApplication, owner: KtAnnotated) -> Boolean
        ): KtRendererAnnotationsFilter = object : KtRendererAnnotationsFilter {
            context(KtAnalysisSession)
            override fun filter(annotation: KtAnnotationApplication, owner: KtAnnotated): Boolean {
                return predicate(this@KtAnalysisSession, annotation, owner)
            }
        }
    }
}
