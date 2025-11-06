/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.load.java

import org.jetbrains.kotlin.load.java.structure.JavaAnnotation
import org.jetbrains.kotlin.load.java.structure.JavaPackage

/**
 * A service that provides an accurate set of annotations on special Java elements.
 * Currently, it only provides annotations on packages, but module annotations may also be supported in the future (see KT-82239).
 */
interface JavaAnnotationProvider {
    /**
     * Returns annotations on the given package, or an empty list if there are none.
     * Returns `null` if the provider is unable to return accurate results.
     *
     * Only annotations present in the resolution scope of the provider's module should be returned.
     * To compare, `PsiPackageImpl.getAnnotations()` uses the `EverythingGlobalScope`, so it returns annotations for all
     * `package-info.java` it can find.
     */
    fun getPackageAnnotations(owner: JavaPackage): List<JavaAnnotation>?

    object Empty : JavaAnnotationProvider {
        override fun getPackageAnnotations(owner: JavaPackage): List<JavaAnnotation>? {
            return null
        }
    }
}