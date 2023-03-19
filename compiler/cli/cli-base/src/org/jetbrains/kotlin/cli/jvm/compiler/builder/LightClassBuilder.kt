/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jvm.compiler.builder

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.impl.compiled.ClsFileImpl
import com.intellij.psi.impl.java.stubs.PsiJavaFileStub
import com.intellij.psi.impl.java.stubs.impl.PsiJavaFileStubImpl
import org.jetbrains.kotlin.asJava.builder.ClsWrapperStubPsiFactory
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.diagnostics.Diagnostics

data class CodeGenerationResult(val stub: PsiJavaFileStub, val bindingContext: BindingContext, val diagnostics: Diagnostics)

fun extraJvmDiagnosticsFromBackend(
    packageFqName: FqName,
    files: Collection<KtFile>,
    generateClassFilter: GenerationState.GenerateClassFilter,
    context: LightClassConstructionContext,
    generate: (state: GenerationState, files: Collection<KtFile>) -> Unit,
): CodeGenerationResult {
    val project = files.first().project

    try {
        val classBuilderFactory = KotlinLightClassBuilderFactory(createJavaFileStub(packageFqName, files))
        val state = GenerationState.Builder(
            project,
            classBuilderFactory,
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

        val javaFileStub = classBuilderFactory.result()
        return CodeGenerationResult(javaFileStub, context.bindingContext, state.collectedExtraJvmDiagnostics)
    } catch (e: ProcessCanceledException) {
        throw e
    } catch (e: RuntimeException) {
        logErrorWithOSInfo(e, packageFqName, files.firstOrNull()?.virtualFile)
        throw e
    }
}

private fun createJavaFileStub(packageFqName: FqName, files: Collection<KtFile>): PsiJavaFileStub {
    val javaFileStub = PsiJavaFileStubImpl(packageFqName.asString(), /* compiled = */true)
    javaFileStub.psiFactory = ClsWrapperStubPsiFactory.INSTANCE

    val fakeFile = object : ClsFileImpl(files.first().viewProvider) {
        override fun getStub() = javaFileStub
        override fun getPackageName() = packageFqName.asString()
        override fun isPhysical() = false
        override fun getText(): String = files.singleOrNull()?.text ?: super.getText()
    }

    javaFileStub.psi = fakeFile
    return javaFileStub
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
