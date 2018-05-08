/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.asJava.builder

import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.compiled.ClsFileImpl
import com.intellij.psi.impl.java.stubs.PsiJavaFileStub
import com.intellij.psi.impl.java.stubs.impl.PsiJavaFileStubImpl
import com.intellij.psi.impl.source.tree.TreeElement
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.diagnostics.Diagnostics
import java.lang.StringBuilder

data class LightClassBuilderResult(val stub: PsiJavaFileStub, val bindingContext: BindingContext, val diagnostics: Diagnostics)

fun buildLightClass(
        packageFqName: FqName,
        files: Collection<KtFile>,
        generateClassFilter: GenerationState.GenerateClassFilter,
        context: LightClassConstructionContext,
        generate: (state: GenerationState, files: Collection<KtFile>) -> Unit
): LightClassBuilderResult {
    val project = files.first().project

    try {
        val classBuilderFactory = KotlinLightClassBuilderFactory(createJavaFileStub(project, packageFqName, files))
        val state = GenerationState.Builder(
                project,
                classBuilderFactory,
                context.module,
                context.bindingContext,
                files.toList(),
                CompilerConfiguration.EMPTY
        ).generateDeclaredClassFilter(generateClassFilter).wantsDiagnostics(false).build()
        state.beforeCompile()

        generate(state, files)

        val javaFileStub = classBuilderFactory.result()

        ServiceManager.getService(project, StubComputationTracker::class.java)?.onStubComputed(javaFileStub, context)
        return LightClassBuilderResult(javaFileStub, context.bindingContext, state.collectedExtraJvmDiagnostics)
    }
    catch (e: ProcessCanceledException) {
        throw e
    }
    catch (e: RuntimeException) {
        logErrorWithOSInfo(e, packageFqName, null)
        throw e
    }
}

private fun createJavaFileStub(project: Project, packageFqName: FqName, files: Collection<KtFile>): PsiJavaFileStub {
    val javaFileStub = PsiJavaFileStubImpl(packageFqName.asString(), /*compiled = */true)
    javaFileStub.psiFactory = ClsWrapperStubPsiFactory.INSTANCE

    val fakeFile = object : ClsFileImpl(files.first().viewProvider) {
        override fun getStub() = javaFileStub

        override fun getPackageName() = packageFqName.asString()

        override fun isPhysical() = false

        override fun appendMirrorText(indentLevel: Int, buffer: StringBuilder) {
            if (files.size == 1) {
                LOG.error("Mirror text should never be calculated for light classes generated from a single file")
            }
            super.appendMirrorText(indentLevel, buffer)
        }


        override fun setMirror(element: TreeElement) {
            if (files.size == 1) {
                LOG.error("Mirror element should never be set for light classes generated from a single file")
            }
            super.setMirror(element)
        }

        override fun getMirror(): PsiElement {
            if (files.size == 1) {
                LOG.error("Mirror element should never be calculated for light classes generated from a single file")
            }
            return super.getMirror()
        }

        override fun getText(): String {
            return files.singleOrNull()?.text ?: super.getText()
        }
    }

    javaFileStub.psi = fakeFile
    return javaFileStub
}

private fun logErrorWithOSInfo(cause: Throwable?, fqName: FqName, virtualFile: VirtualFile?) {
    val path = if (virtualFile == null) "<null>" else virtualFile.path
    LOG.error(
            "Could not generate LightClass for $fqName declared in $path\n" +
            "System: ${SystemInfo.OS_NAME} ${SystemInfo.OS_VERSION} Java Runtime: ${SystemInfo.JAVA_RUNTIME_VERSION}",
            cause
    )
}

private val LOG = Logger.getInstance(LightClassBuilderResult::class.java)