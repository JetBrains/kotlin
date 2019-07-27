/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.perf

import com.intellij.psi.PsiDocumentManager
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.resolveImportReference
import org.jetbrains.kotlin.idea.util.ImportDescriptorResult
import org.jetbrains.kotlin.idea.util.ImportInsertHelper
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.renderer.DescriptorRenderer

/**
 * inspired by @see org.jetbrains.kotlin.addImport.AbstractAddImportTest
 */
abstract class AbstractPerformanceAddImportTest : AbstractPerformanceImportTest() {
    companion object {

        @JvmStatic
        val stats: Stats = Stats("add-import")

        init {
            // there is no @AfterClass for junit3.8
            Runtime.getRuntime().addShutdownHook(Thread(Runnable { stats.close() }))
        }

    }

    override fun stats(): Stats = stats

    override fun perfTestCore(
        file: KtFile,
        fqName: FqName,
        filter: (DeclarationDescriptor) -> Boolean,
        descriptorName: String,
        importInsertHelper: ImportInsertHelper,
        psiDocumentManager: PsiDocumentManager
    ): String? {
        val resolveImportReference = file.resolveImportReference(fqName)
        val descriptors = resolveImportReference.filter(filter)

        return when {
            descriptors.isEmpty() -> error("No descriptor $descriptorName found")

            descriptors.size > 1 ->
                error(
                    "Multiple descriptors found:\n    " +
                            descriptors.joinToString("\n    ") { DescriptorRenderer.FQ_NAMES_IN_TYPES.render(it) }
                )

            else -> {
                val success =
                    importInsertHelper.importDescriptor(file, descriptors.single()) != ImportDescriptorResult.FAIL
                if (!success) {
                    val document = psiDocumentManager.getDocument(file)!!
                    document.replaceString(0, document.textLength, "Failed to add import")
                    psiDocumentManager.commitAllDocuments()
                }
                null
            }
        }
    }
}