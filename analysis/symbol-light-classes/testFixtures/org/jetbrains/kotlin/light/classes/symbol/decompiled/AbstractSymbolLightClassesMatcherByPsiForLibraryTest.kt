/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.decompiled

import com.intellij.psi.PsiClass
import org.jetbrains.kotlin.analysis.decompiler.psi.file.KtClsFile
import org.jetbrains.kotlin.asJava.KotlinAsJavaSupport
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtDeclaration

abstract class AbstractSymbolLightClassesMatcherByPsiForLibraryTest : AbstractSymbolLightClassesMatcherForLibraryTest() {
    override fun collectDeclarationsToMatch(file: KtClsFile): MutableMap<KtDeclaration, Boolean> {
        return collectDeclarationsRecursively(file)
    }

    override fun collectLightClassesToMatch(file: KtClsFile): List<PsiClass> {
        val javaSupport = KotlinAsJavaSupport.getInstance(file.project)
        return buildList {
            file.declarations.filterIsInstance<KtClassOrObject>().forEach { classOrObject ->
                javaSupport.getLightClass(classOrObject)?.let { add(it) }
            }
            javaSupport.getLightFacade(file)?.let { add(it) }
        }
    }
}
