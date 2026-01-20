/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi

import com.intellij.openapi.util.Key
import com.intellij.psi.PsiAnnotation

val DEBUG_KEY = Key.create<DebugUserData>("DEBUG_KEY")

class DebugUserData(
    val annotationsBox: String,
    val annoBoxCreationThrowable: Throwable? = null,
    val annoBoxPopulationThrowable: Throwable? = null,
    val getCachedAnnotations: (() -> Collection<PsiAnnotation>?)? = null,
    val getExtraAnnotationInfo: (() -> String)? = null,
)
