/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.parsing.util

import com.intellij.core.CoreApplicationEnvironment
import com.intellij.core.CoreProjectEnvironment
import com.intellij.lang.MetaLanguage
import com.intellij.openapi.Disposable
import com.intellij.openapi.extensions.Extensions
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.local.CoreLocalFileSystem
import com.intellij.psi.PsiManager
import com.intellij.psi.SingleRootFileViewProvider
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.parsing.KotlinParserDefinition
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.script.KotlinScriptDefinition
import org.jetbrains.kotlin.script.ScriptDefinitionProvider

import java.io.File
import java.util.*

fun classesFqNames(files: Set<File>): Set<String> = withPsiSetup {
    val psiManager = PsiManager.getInstance(project)
    val fileManager = VirtualFileManager.getInstance()
    val localFS = fileManager.getFileSystem(StandardFileSystems.FILE_PROTOCOL) as CoreLocalFileSystem

    classesFqNames(files, psiManager, localFS)
}

private fun classesFqNames(files: Collection<File>, psiManager: PsiManager, localFS: CoreLocalFileSystem): Set<String> {
    val result = HashSet<String>()

    for (file in files) {
        if (!file.name.endsWith(".kt", ignoreCase = true)) continue

        val virtualFile = localFS.findFileByIoFile(file) ?: continue

        for (psiFile in SingleRootFileViewProvider(psiManager, virtualFile).allFiles) {
            if (psiFile !is KtFile) continue

            val classes = ArrayDeque<KtClassOrObject>()
            psiFile.declarations.filterClassesTo(classes)
            while (classes.isNotEmpty()) {
                val klass = classes.pollFirst()
                klass.fqName?.let {
                    result.add(it.asString())
                }
                klass.declarations.filterClassesTo(classes)
            }
        }
    }

    return result
}

private fun Collection<KtDeclaration>.filterClassesTo(classes: Deque<KtClassOrObject>) {
    filterIsInstanceTo<KtClassOrObject, Deque<KtClassOrObject>>(classes)
}

private data class PsiSetup(
    val applicationEnvironment: CoreApplicationEnvironment,
    val projectEnvironment: CoreProjectEnvironment,
    val project: Project,
    val disposable: Disposable
)

private inline fun <T> withPsiSetup(fn: PsiSetup.() -> T): T {
    val disposable = Disposer.newDisposable()

    return try {
        val applicationEnvironment = CoreApplicationEnvironment(disposable, false)
        val projectEnvironment = CoreProjectEnvironment(disposable, applicationEnvironment)
        CoreApplicationEnvironment.registerExtensionPoint(Extensions.getRootArea(), MetaLanguage.EP_NAME, MetaLanguage::class.java)
        applicationEnvironment.registerApplicationService(ScriptDefinitionProvider::class.java, NoopScriptDefinitionProvider())
        applicationEnvironment.registerFileType(KotlinFileType.INSTANCE, "kt")
        applicationEnvironment.registerParserDefinition(KotlinParserDefinition())

        val project = projectEnvironment.project
        val setup = PsiSetup(applicationEnvironment, projectEnvironment, project, disposable)
        setup.fn()
    } finally {
        Disposer.dispose(disposable)
    }
}

private class NoopScriptDefinitionProvider : ScriptDefinitionProvider {
    override fun isScript(fileName: String): Boolean {
        return false
    }

    override fun findScriptDefinition(fileName: String): KotlinScriptDefinition? {
        return null
    }
}