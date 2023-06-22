/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.common

import com.intellij.ide.highlighter.JavaFileType
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.KtSourceFile
import org.jetbrains.kotlin.KtVirtualFileSourceFile
import org.jetbrains.kotlin.cli.common.config.kotlinSourceRoots
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.jvm.compiler.VfsBasedProjectEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.forAllFiles
import org.jetbrains.kotlin.cli.jvm.compiler.getSourceRootsCheckingForDuplicates
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.idea.KotlinFileType
import java.util.TreeSet

private const val kotlinFileExtensionWithDot = ".${KotlinFileType.EXTENSION}"
private const val javaFileExtensionWithDot = ".${JavaFileType.DEFAULT_EXTENSION}"

data class GroupedKtSources(
    val platformSources: Collection<KtSourceFile>,
    val commonSources: Collection<KtSourceFile>,
    val sourcesByModuleName: Map<String, Set<KtSourceFile>>,
) {
    fun isEmpty(): Boolean = platformSources.isEmpty() && commonSources.isEmpty()
}

fun collectSources(
    compilerConfiguration: CompilerConfiguration,
    projectEnvironment: VfsBasedProjectEnvironment,
    messageCollector: MessageCollector
): GroupedKtSources {
    return collectSources(compilerConfiguration, projectEnvironment.project, messageCollector)
}

private val ktSourceFileComparator = Comparator<KtSourceFile> { o1, o2 ->
    val path1 = o1.path ?: error("Expected a file with a well-defined path")
    val path2 = o2.path ?: error("Expected a file with a well-defined path")
    path1.compareTo(path2)
}

fun collectSources(
    compilerConfiguration: CompilerConfiguration,
    project: Project,
    messageCollector: MessageCollector
): GroupedKtSources {
    val platformSources = TreeSet(ktSourceFileComparator)
    val commonSources = TreeSet(ktSourceFileComparator)
    val sourcesByModuleName = mutableMapOf<String, MutableSet<KtSourceFile>>()

    // TODO: the scripts checking should be part of the scripting plugin functionality, as it is implemented now in ScriptingProcessSourcesBeforeCompilingExtension
    // TODO: implement in the next round of K2 scripting support (https://youtrack.jetbrains.com/issue/KT-55728)
    val skipScriptsInLtMode = compilerConfiguration.getBoolean(CommonConfigurationKeys.USE_FIR) &&
            compilerConfiguration.getBoolean(CommonConfigurationKeys.USE_LIGHT_TREE)
    var skipScriptsInLtModeWarning = false

    getSourceRootsCheckingForDuplicates(compilerConfiguration, messageCollector).forAllFiles(
        compilerConfiguration,
        project
    ) { virtualFile, isCommon, moduleName ->
        val file = KtVirtualFileSourceFile(virtualFile)
        when {
            file.path.endsWith(javaFileExtensionWithDot) -> {}
            file.path.endsWith(kotlinFileExtensionWithDot) || !skipScriptsInLtMode -> {
                if (isCommon) commonSources.add(file)
                else platformSources.add(file)

                if (moduleName != null) {
                    sourcesByModuleName.getOrPut(moduleName) { mutableSetOf() }.add(file)
                }
            }
            else -> {
                // temporarily assume it is a script, see the TODO above
                skipScriptsInLtModeWarning = true
            }
        }
    }

    if (skipScriptsInLtModeWarning) {
        // TODO: remove then Scripts are supported in LT (probably different K2 extension should be written for handling the case properly)
        messageCollector.report(
            CompilerMessageSeverity.STRONG_WARNING,
            "Scripts are not yet supported with K2 in LightTree mode, consider using K1 or disable LightTree mode with -Xuse-fir-lt=false"
        )
    }
    return GroupedKtSources(platformSources, commonSources, sourcesByModuleName)
}
