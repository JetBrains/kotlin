/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.asJava

import com.intellij.psi.PsiClass
import org.jetbrains.annotations.ApiStatus

/**
 * This is an implementation detail interface intended to be used only as
 * the implementation for [org.jetbrains.kotlin.load.java.structure.JavaClass.originalClsJavaClass]
 */
@ApiStatus.Internal
interface KtClsJavaBasedLightClass {
    val clsDelegate: PsiClass
}
