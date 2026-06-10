/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java

import org.jetbrains.kotlin.KtRealSourceElementKind
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.KtSourceElementKind

/**
 * Seam interface for obtaining non-PSI-based source elements in [JavaElement.toSourceElement]. Used in the `java-direct` module.
 */
interface JavaDirectSourceElementOwner {
    fun toKtSourceElement(kind: KtSourceElementKind = KtRealSourceElementKind): KtSourceElement?
}
