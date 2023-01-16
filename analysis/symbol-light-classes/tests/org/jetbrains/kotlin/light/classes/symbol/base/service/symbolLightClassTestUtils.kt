/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.base.service

import org.jetbrains.kotlin.asJava.PsiClassRenderer
import java.nio.file.Path

internal inline fun <R> withExtendedTypeRenderer(testDataFile: Path, action: () -> R): R {
    val extendedTypeRendererOld = PsiClassRenderer.extendedTypeRenderer
    return try {
        PsiClassRenderer.extendedTypeRenderer = testDataFile.toString().endsWith("typeAnnotations.kt")
        action()
    } finally {
        PsiClassRenderer.extendedTypeRenderer = extendedTypeRendererOld
    }
}