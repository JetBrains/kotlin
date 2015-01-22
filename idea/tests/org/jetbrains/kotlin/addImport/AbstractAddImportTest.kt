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

import org.jetbrains.kotlin.AbstractImportsTest
import org.jetbrains.kotlin.psi.JetFile
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.quickfix.ImportInsertHelper
import org.jetbrains.kotlin.psi.JetPsiFactory
import org.jetbrains.kotlin.resolve.QualifiedExpressionResolver
import org.jetbrains.kotlin.resolve.QualifiedExpressionResolver.LookupMode
import org.jetbrains.kotlin.resolve.JetModuleUtil
import org.jetbrains.kotlin.resolve.BindingTraceContext
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import com.intellij.psi.PsiDocumentManager

public abstract class AbstractAddImportTest : AbstractImportsTest() {
    override fun doTest(file: JetFile) {
        var descriptorName = InTextDirectivesUtils.findStringWithPrefixes(file.getText(), "// IMPORT:")
                             ?: error("No IMPORT directive defined")

        var filter: (DeclarationDescriptor) -> Boolean = { true }
        if (descriptorName.startsWith("class:")) {
            filter = { it is ClassDescriptor }
            descriptorName = descriptorName.substring("class:".length()).trim()
        }

        val importDirective = JetPsiFactory(getProject()).createImportDirective(descriptorName)
        val moduleDescriptor = file.getResolutionFacade().findModuleDescriptor(file)
        val scope = JetModuleUtil.getSubpackagesOfRootScope(moduleDescriptor)
        val descriptors = QualifiedExpressionResolver()
                .processImportReference(importDirective, scope, scope, null, BindingTraceContext(), LookupMode.EVERYTHING)
                .filter(filter)

        when {
            descriptors.isEmpty() ->
                error("No descriptor $descriptorName found")

            descriptors.size() > 1 ->
                error("Multiple descriptors found:\n    " + descriptors.map { DescriptorRenderer.FQ_NAMES_IN_TYPES.render(it) }.joinToString("\n    "))

            else -> {
                val success = ImportInsertHelper.INSTANCE.importDescriptor(file, descriptors.single())
                if (!success) {
                    val document = PsiDocumentManager.getInstance(getProject()).getDocument(file)
                    document.replaceString(0, document.getTextLength(), "Failed to add import")
                    PsiDocumentManager.getInstance(getProject()).commitAllDocuments()
                }
            }
        }
    }
}