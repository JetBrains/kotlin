/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.configurators

import com.intellij.core.CoreApplicationEnvironment
import com.intellij.lang.ASTNode
import com.intellij.mock.MockApplication
import com.intellij.mock.MockFileDocumentManagerImpl
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.impl.DocumentImpl
import com.intellij.openapi.editor.impl.DocumentWriteAccessGuard
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.impl.FileDocumentManagerBase
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.psi.impl.source.codeStyle.IndentHelper
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreApplicationEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreApplicationEnvironmentConfigurator

/**
 * Registers application services which are required for PSI modification operations (such as deleting a PSI element) to complete without
 * throwing exceptions.
 *
 * @see AnalysisApiModifiablePsiTestProjectConfigurator
 */
object AnalysisApiModifiablePsiTestApplicationEnvironmentConfiguration : AnalysisApiBaseTestApplicationEnvironmentConfiguration() {
    init {
        addConfigurator(AnalysisApiModifiablePsiTestApplicationEnvironmentConfigurator)
    }
}

private object AnalysisApiModifiablePsiTestApplicationEnvironmentConfigurator : KotlinCoreApplicationEnvironmentConfigurator {
    override fun configure(applicationEnvironment: KotlinCoreApplicationEnvironment) {
        applicationEnvironment.application.apply {
            registerFileDocumentManager()

            registerService(IndentHelper::class.java, MockIndentHelper::class.java)

            CoreApplicationEnvironment.registerExtensionPoint(
                extensionArea,
                DocumentWriteAccessGuard.EP_NAME,
                MockDocumentWriteAccessGuard::class.java,
            )
        }
    }

    /**
     * [MockFileDocumentManagerImpl] doesn't put the cached document as user data under [MockFileDocumentManagerImpl.myCachedDocumentKey] on
     * the virtual file, making [MockFileDocumentManagerImpl.getCachedDocument] effectively return `null`. We extend the file document
     * manager to simulate the behavior of [FileDocumentManagerBase], which puts [FileDocumentManagerBase.HARD_REF_TO_DOCUMENT_KEY] user
     * data on the virtual file.
     */
    private fun MockApplication.registerFileDocumentManager() {
        // To register our own file document manager, we need to remove the manager already registered with our application at this point.
        picoContainer.unregisterComponent(FileDocumentManager::class.java.name)

        registerService(
            FileDocumentManager::class.java,
            object : MockFileDocumentManagerImpl(FileDocumentManagerBase.HARD_REF_TO_DOCUMENT_KEY, { DocumentImpl(it) }) {
                override fun getDocument(file: VirtualFile): Document? {
                    val document = super.getDocument(file) ?: return null
                    file.putUserDataIfAbsent(FileDocumentManagerBase.HARD_REF_TO_DOCUMENT_KEY, document)
                    return document
                }
            },
        )
    }
}

@Suppress("UnstableApiUsage")
private class MockDocumentWriteAccessGuard : DocumentWriteAccessGuard() {
    override fun isWritable(document: Document): Result = success()
}

private class MockIndentHelper : IndentHelper() {
    override fun getIndent(file: PsiFile, element: ASTNode): Int = 0
    override fun getIndent(file: PsiFile, element: ASTNode, includeNonSpace: Boolean): Int = 0
}
