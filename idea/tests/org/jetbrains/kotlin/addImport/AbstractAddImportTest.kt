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

package org.jetbrains.kotlin.addImport

import com.intellij.psi.PsiDocumentManager
import org.jetbrains.kotlin.AbstractImportsTest
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.resolveImportReference
import org.jetbrains.kotlin.idea.util.ImportDescriptorResult
import org.jetbrains.kotlin.idea.util.ImportInsertHelper
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.test.InTextDirectivesUtils

abstract class AbstractAddImportTest : AbstractImportsTest() {
    override fun doTest(file: KtFile): String? {
        var descriptorName = InTextDirectivesUtils.findStringWithPrefixes(file.text, "// IMPORT:")
                             ?: error("No IMPORT directive defined")

        var filter: (DeclarationDescriptor) -> Boolean = { true }
        if (descriptorName.startsWith("class:")) {
            filter = { it is ClassDescriptor }
            descriptorName = descriptorName.substring("class:".length).trim()
        }

        val descriptors = file.resolveImportReference(FqName(descriptorName)).filter(filter)

        when {
            descriptors.isEmpty() ->
                error("No descriptor $descriptorName found")

            descriptors.size > 1 ->
                error("Multiple descriptors found:\n    " + descriptors.map { DescriptorRenderer.FQ_NAMES_IN_TYPES.render(it) }.joinToString("\n    "))

            else -> {
                val success = ImportInsertHelper.getInstance(project).importDescriptor(file, descriptors.single()) != ImportDescriptorResult.FAIL
                if (!success) {
                    val document = PsiDocumentManager.getInstance(project).getDocument(file)!!
                    document.replaceString(0, document.textLength, "Failed to add import")
                    PsiDocumentManager.getInstance(project).commitAllDocuments()
                }
            }
        }

        return null
    }
}