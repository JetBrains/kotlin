/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.resolver

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReferenceService
import org.jetbrains.kotlin.analysis.test.framework.project.structure.ktTestModuleStructure
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.TestModuleKind
import org.jetbrains.kotlin.idea.references.KtReference
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.forEachDescendantOfTypeInPreorder
import org.jetbrains.kotlin.test.services.TestServices

/**
 * This test case is supposed to dump all resolution information such as symbols, calls, and call candidates from a file.
 */
abstract class AbstractResolveByPsiTest : AbstractMultiResolveTest() {
    override fun process(testServices: TestServices, consumer: (Any) -> Unit) {
        val filesToProcess = testServices.ktTestModuleStructure
            .mainModules
            .filter {
                when (it.moduleKind) {
                    TestModuleKind.Source, TestModuleKind.ScriptSource, TestModuleKind.LibrarySource, TestModuleKind.CodeFragment -> true
                    else -> false
                }
            }
            .flatMap { it.ktFiles }

        if (filesToProcess.isEmpty()) error("Where are no files to process")

        val referenceService = PsiReferenceService.getService()
        for (file in filesToProcess) {
            file.forEachDescendantOfTypeInPreorder<PsiElement> { element ->
                if (element is KtElement) {
                    consumer(element)
                }

                for (reference in referenceService.getContributedReferences(element)) {
                    if (reference is KtReference) {
                        consumer(reference)
                    }
                }
            }
        }
    }
}
