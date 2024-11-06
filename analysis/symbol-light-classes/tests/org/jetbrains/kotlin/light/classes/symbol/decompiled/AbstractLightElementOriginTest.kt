/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.decompiled

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.asJava.elements.KtLightElement
import org.jetbrains.kotlin.light.classes.symbol.base.AbstractSymbolLightClassesByPsiTest
import org.jetbrains.kotlin.light.classes.symbol.decompiled.test.configurators.AnalysisApiSymbolLightClassesDecompiledTestConfigurator
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.psi.KtElement


abstract class AbstractLightElementOriginTest : AbstractSymbolLightClassesByPsiTest(
    AnalysisApiSymbolLightClassesDecompiledTestConfigurator(JvmPlatforms.defaultJvmPlatform),
    EXTENSIONS.LIB_JAVA,
    isTestAgainstCompiledCode = true,
) {
    override fun additionalInformation(element: PsiElement): String? {
        return when (element) {
            is KtLightElement<*, *> -> {
                "(origin: ${element.kotlinOrigin?.toOriginString()})"
            }
            else -> {
                null
            }
        }
    }


    private fun KtElement.toOriginString(): String =
        "$this ${this.name} in ${this.containingKtFile.virtualFile.name}"
}
