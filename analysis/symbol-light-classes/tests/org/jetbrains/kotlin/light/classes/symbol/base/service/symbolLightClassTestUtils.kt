/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.base.service

import com.intellij.psi.PsiClass
import com.intellij.psi.SyntaxTraverser
import org.jetbrains.kotlin.asJava.PsiClassRenderer
import org.jetbrains.kotlin.asJava.toLightClass
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
    return ktClasses.plus(ktFile).flatMap { ktElement ->
        if (ktElement is KtFile && ktElement.isScript()) {
            // Regular [KtElement.toLightElements] will attempt to find a facade class for [KtFile],
            // where we deliberately drop .kts as per KTIJ-22016
            // Thus, we need to invoke [KtScript.toLightClass] explicitly.
            // That's how (U)LC tests do too: see [AbstractIdeLightClassesByPsiTest#doMultiFileTest]
            listOfNotNull(ktElement.script?.toLightClass())
        } else {
            ktElement.toLightElements()
        }
    }.filterIsInstance<PsiClass>()
}
