/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.test

import com.intellij.codeInsight.ImportFilter
import com.intellij.openapi.application.ApplicationManager
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.test.InTextDirectivesUtils

object KotlinTestImportFilter : ImportFilter() {
    override fun shouldUseFullyQualifiedName(targetFile: PsiFile, classQualifiedName: String): Boolean {
        if (!ApplicationManager.getApplication().isUnitTestMode) {
            return false
        }
        val doNotImport = InTextDirectivesUtils.findLinesWithPrefixesRemoved(targetFile.text, "// DO_NOT_IMPORT:")
        return classQualifiedName in doNotImport
    }
}