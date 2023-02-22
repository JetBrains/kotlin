/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.base.service

import com.intellij.psi.PsiClass
import com.intellij.psi.SyntaxTraverser
import org.jetbrains.kotlin.asJava.PsiClassRenderer
import org.jetbrains.kotlin.asJava.toLightElements
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile
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

internal fun getLightClassesFromFile(ktFile: KtFile): List<PsiClass> {
    val ktClasses = SyntaxTraverser.psiTraverser(ktFile).filter(KtClassOrObject::class.java).toList()
    return ktClasses.plus(ktFile).flatMap { it.toLightElements() }.filterIsInstance<PsiClass>()
}
