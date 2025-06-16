/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.annotations

import com.intellij.psi.PsiAnnotation
import org.jetbrains.kotlin.name.JvmStandardClassIds

/**
 * Filter out [qualifiedName] annotations.
 *
 * For instance, after compilation, the [kotlin.jvm.JvmExposeBoxed] annotation is not present on classes or unboxed versions of declarations
 */
internal open class ExcludeAnnotationFilter(private val qualifiedName: String) : AnnotationFilter {
    override fun isAllowed(qualifiedName: String): Boolean {
        return qualifiedName != this.qualifiedName
    }

    override fun filtered(annotations: Collection<PsiAnnotation>): Collection<PsiAnnotation> = annotations.filterNot {
        it.hasQualifiedName(qualifiedName)
    }

    internal object JvmExposeBoxed : ExcludeAnnotationFilter(JvmStandardClassIds.JVM_EXPOSE_BOXED_ANNOTATION_FQ_NAME.asString())
    internal object JvmName : ExcludeAnnotationFilter(JvmStandardClassIds.JVM_NAME.asString())
}
