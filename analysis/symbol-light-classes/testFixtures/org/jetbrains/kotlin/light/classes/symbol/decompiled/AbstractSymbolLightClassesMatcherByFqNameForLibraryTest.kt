/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.decompiled

import com.intellij.psi.PsiClass
import org.jetbrains.kotlin.analysis.decompiler.psi.file.KtClsFile
import org.jetbrains.kotlin.asJava.LightClassTestCommon
import org.jetbrains.kotlin.asJava.unwrapped
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtDeclarationContainer

abstract class AbstractSymbolLightClassesMatcherByFqNameForLibraryTest : AbstractSymbolLightClassesMatcherForLibraryTest() {
    override fun collectDeclarationsToMatch(file: KtClsFile): MutableMap<KtDeclaration, Boolean> {
        val lightClass = findTargetLightClass(file) ?: return mutableMapOf()
        val root = lightClass.unwrapped as? KtDeclarationContainer ?: return mutableMapOf()
        return collectDeclarationsRecursively(root)
    }

    override fun collectLightClassesToMatch(file: KtClsFile): List<PsiClass> {
        return listOfNotNull(findTargetLightClass(file))
    }

    private fun findTargetLightClass(file: KtClsFile): PsiClass? {
        val fqName = LightClassTestCommon.fqNameInTestDataFile(testDataPath.toFile())
        val lightClass = findLightClass(fqName, file) ?: return null
        // `findLightClass` falls back to project-wide search if no match is found in the given file,
        // but `ktFiles` may contain several `KtClsFile`s (one per top-level class in the source).
        // Restrict matching to the file that actually contains the FQ-name'd class so that other
        // sibling `.class` files in the same source do not get spurious mismatch reports.
        return lightClass.takeIf { it.unwrapped?.containingFile == file }
    }
}
