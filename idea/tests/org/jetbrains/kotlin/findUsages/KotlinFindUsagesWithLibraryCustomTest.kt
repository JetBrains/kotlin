/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.findUsages

import com.intellij.psi.search.FilenameIndex
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType

class KotlinFindUsagesWithLibraryCustomTest : AbstractKotlinFindUsagesWithLibraryTest() {
    fun testFindUsagesForLocalClassProperty() {
        val libraryFile = FilenameIndex.getFilesByName(getProject(), "library.kt", myFixture.module.moduleWithLibrariesScope).first()
        val indexOf = libraryFile.text.indexOf("localClassProperty")
        val jetParameter = libraryFile.findElementAt(indexOf)!!.getStrictParentOfType<KtParameter>()!!
        val usages = findUsages(jetParameter.getOriginalElement(), null, false, project)
        assertEquals(2, usages.size)
    }
}