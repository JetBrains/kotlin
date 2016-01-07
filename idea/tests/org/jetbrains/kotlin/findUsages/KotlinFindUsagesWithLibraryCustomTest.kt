/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.findUsages

import com.intellij.psi.search.FilenameIndex
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import kotlin.test.assertEquals

class KotlinFindUsagesWithLibraryCustomTest : AbstractKotlinFindUsagesWithLibraryTest() {
    fun testFindUsagesForLocalClassProperty() {
        val libraryFile = FilenameIndex.getFilesByName(getProject(), "library.kt", myFixture.module.moduleWithLibrariesScope).first()
        val indexOf = libraryFile.text.indexOf("localClassProperty")
        val jetParameter = libraryFile.findElementAt(indexOf)!!.getStrictParentOfType<KtParameter>()!!
        val usages = findUsages(jetParameter.getOriginalElement(), null, false)
        assertEquals(2, usages.size)
    }
}