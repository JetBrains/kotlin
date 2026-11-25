/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental.parsing

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiManager
import com.intellij.psi.SingleRootFileViewProvider
import org.jetbrains.kotlin.CoreEnvironmentDeprecation
import org.jetbrains.kotlin.cli.common.disposeRootInWriteAction
import org.jetbrains.kotlin.cli.common.localfs.KotlinLocalFileSystem
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.create
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.config.configureJdkClasspathRoots
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.config.MessageCollectorAccess
import org.jetbrains.kotlin.config.messageCollector
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.syntax.PsiParseTreeKind
import java.io.File
import java.util.*

fun classesFqNames(files: Set<File>): Set<String> {
    val existingKotlinFiles = files.filter { it.name.endsWith(".kt", ignoreCase = true) && it.isFile }
    if (existingKotlinFiles.isEmpty()) return emptySet()

    val disposable = Disposer.newDisposable("Disposable for org.jetbrains.kotlin.incremental.parsing.classesFqNames")

    return try {
        classesFqNames(existingKotlinFiles, disposable)
    } finally {
        disposeRootInWriteAction(disposable)
    }
}

private fun classesFqNames(kotlinFiles: Collection<File>, disposable: Disposable): Set<String> {
    // The class names are collected from the PSI of the Kotlin files, KT-91411
    val config = CompilerConfiguration.create(parseTreeKind = PsiParseTreeKind)
    config.put(JVMConfigurationKeys.NO_JDK, true)
    @OptIn(MessageCollectorAccess::class) // write access
    config.messageCollector = MessageCollector.NONE
    config.configureJdkClasspathRoots()

    @OptIn(CoreEnvironmentDeprecation::class)
    val environment = KotlinCoreEnvironment.create(disposable, config)
    val psiManager = PsiManager.getInstance(environment.project)
    val fileManager = VirtualFileManager.getInstance()
    val localFS = fileManager.getFileSystem(StandardFileSystems.FILE_PROTOCOL) as KotlinLocalFileSystem

    val result = HashSet<String>()

    for (file in kotlinFiles) {
        val virtualFile = localFS.findFileByIoFile(file)!!

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
