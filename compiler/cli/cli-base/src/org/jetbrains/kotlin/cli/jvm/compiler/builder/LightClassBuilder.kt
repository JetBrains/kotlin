/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jvm.compiler.builder

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.diagnostics.Diagnostics

data class CodeGenerationResult(val bindingContext: BindingContext, val diagnostics: Diagnostics)

fun extraJvmDiagnosticsFromBackend(
    packageFqName: FqName,
    files: Collection<KtFile>,
    generateClassFilter: GenerationState.GenerateClassFilter,
    context: LightClassConstructionContext,
    generate: (state: GenerationState, files: Collection<KtFile>) -> Unit,
): CodeGenerationResult {
    val project = files.first().project

    try {
        val state = GenerationState.Builder(
            project,
            KotlinLightClassBuilderFactory,
            context.module,
            context.bindingContext,
            context.languageVersionSettings?.let {
                CompilerConfiguration().apply {
                    languageVersionSettings = it
                    put(JVMConfigurationKeys.JVM_TARGET, context.jvmTarget)
                    isReadOnly = true
                }
            } ?: CompilerConfiguration.EMPTY,
        ).generateDeclaredClassFilter(generateClassFilter).wantsDiagnostics(false).build()
        state.beforeCompile()
        state.oldBEInitTrace(files)

        generate(state, files)

        return CodeGenerationResult(context.bindingContext, state.collectedExtraJvmDiagnostics)
    } catch (e: ProcessCanceledException) {
        throw e
    } catch (e: RuntimeException) {
        logErrorWithOSInfo(e, packageFqName, files.firstOrNull()?.virtualFile)
        throw e
    }
}

private fun logErrorWithOSInfo(cause: Throwable?, fqName: FqName, virtualFile: VirtualFile?) {
    val path = virtualFile?.path ?: "<null>"
    LOG.error(
        "Could not generate LightClass for $fqName declared in $path\n" +
                "System: ${SystemInfo.OS_NAME} ${SystemInfo.OS_VERSION} Java Runtime: ${SystemInfo.JAVA_RUNTIME_VERSION}",
        cause,
    )
}

private val LOG = Logger.getInstance(CodeGenerationResult::class.java)
