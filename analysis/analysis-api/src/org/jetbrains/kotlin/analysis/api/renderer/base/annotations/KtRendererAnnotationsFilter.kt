/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.renderer.base.annotations

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotated
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationApplication
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.load.java.NULLABILITY_ANNOTATIONS

public interface KaRendererAnnotationsFilter {
    public fun filter(analysisSession: KaSession, annotation: KaAnnotationApplication, owner: KaAnnotated): Boolean

    public infix fun and(other: KaRendererAnnotationsFilter): KaRendererAnnotationsFilter =
        KaRendererAnnotationsFilter filter@{ annotation, owner ->
            val analysisSession = this@filter
            filter(analysisSession, annotation, owner) && other.filter(analysisSession, annotation, owner)
        }

    public infix fun or(other: KaRendererAnnotationsFilter): KaRendererAnnotationsFilter =
        KaRendererAnnotationsFilter filter@{ annotation, owner ->
            val analysisSession = this@filter
            filter(analysisSession, annotation, owner) || other.filter(analysisSession, annotation, owner)
        }

    public object ALL : KaRendererAnnotationsFilter {
        override fun filter(analysisSession: KaSession, annotation: KaAnnotationApplication, owner: KaAnnotated): Boolean {
            return true
        }
    }

    public object NO_NULLABILITY : KaRendererAnnotationsFilter {
        override fun filter(analysisSession: KaSession, annotation: KaAnnotationApplication, owner: KaAnnotated): Boolean {
            return annotation.classId?.asSingleFqName() !in NULLABILITY_ANNOTATIONS
        }
    }

    public object NO_PARAMETER_NAME : KaRendererAnnotationsFilter {
        override fun filter(analysisSession: KaSession, annotation: KaAnnotationApplication, owner: KaAnnotated): Boolean {
            return annotation.classId?.asSingleFqName() != StandardNames.FqNames.parameterName
        }
    }


    public object NONE : KaRendererAnnotationsFilter {
        override fun filter(analysisSession: KaSession, annotation: KaAnnotationApplication, owner: KaAnnotated): Boolean {
            return false
        }
    }

    public companion object {
        public operator fun invoke(
            predicate: KaSession.(annotation: KaAnnotationApplication, owner: KaAnnotated) -> Boolean
        ): KaRendererAnnotationsFilter = object : KaRendererAnnotationsFilter {
            override fun filter(analysisSession: KaSession, annotation: KaAnnotationApplication, owner: KaAnnotated): Boolean {
                return predicate(analysisSession, annotation, owner)
            }
        }
    }
}

public typealias KtRendererAnnotationsFilter = KaRendererAnnotationsFilter