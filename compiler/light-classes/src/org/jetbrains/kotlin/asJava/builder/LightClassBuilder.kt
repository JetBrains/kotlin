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
import com.intellij.psi.ClassFileViewProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.impl.compiled.ClsFileImpl
import com.intellij.psi.impl.java.stubs.PsiJavaFileStub
import com.intellij.psi.impl.java.stubs.impl.PsiJavaFileStubImpl
import com.intellij.psi.stubs.StubElement
import com.intellij.util.containers.Stack
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.diagnostics.Diagnostics

data class LightClassBuilderResult(val stub: PsiJavaFileStub, val bindingContext: BindingContext, val diagnostics: Diagnostics)

fun buildLightClass(
        project: Project,
        packageFqName: FqName,
        files: Collection<KtFile>,
        generateClassFilter: GenerationState.GenerateClassFilter,
        context: LightClassConstructionContext,
        generate: (state: GenerationState, files: Collection<KtFile>) -> Unit
): LightClassBuilderResult {

    val javaFileStub = createJavaFileStub(project, packageFqName, files)
    val bindingContext: BindingContext

    val state: GenerationState

    try {
        val stubStack = Stack<StubElement<PsiElement>>()

        @Suppress("UNCHECKED_CAST")
        stubStack.push(javaFileStub as StubElement<PsiElement>)

        state = GenerationState(
                project,
                KotlinLightClassBuilderFactory(stubStack),
                context.module,
                context.bindingContext,
                files.toMutableList(),
                CompilerConfiguration.EMPTY,
                generateClassFilter,
                wantsDiagnostics = false
        )
        state.beforeCompile()

        bindingContext = state.bindingContext

        generate(state, files)

        val pop = stubStack.pop()
        if (pop !== javaFileStub) {
            LOG.error("Unbalanced stack operations: " + pop)
        }

        ServiceManager.getService(project, StubComputationTracker::class.java)?.onStubComputed(javaFileStub)
        return LightClassBuilderResult(javaFileStub, bindingContext, state.collectedExtraJvmDiagnostics)
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
    val javaFileStub = PsiJavaFileStubImpl(packageFqName.asString(), true)
    javaFileStub.psiFactory = ClsWrapperStubPsiFactory.INSTANCE

    val manager = PsiManager.getInstance(project)

    val virtualFile = getRepresentativeVirtualFile(files)
    val fakeFile = object : ClsFileImpl(ClassFileViewProvider(manager, virtualFile)) {
        override fun getStub() = javaFileStub

        override fun getPackageName() = packageFqName.asString()

        override fun isPhysical() = false
    }

    javaFileStub.psi = fakeFile
    return javaFileStub
}

private fun getRepresentativeVirtualFile(files: Collection<KtFile>): VirtualFile {
    return files.first().viewProvider.virtualFile
}

private fun logErrorWithOSInfo(cause: Throwable?, fqName: FqName, virtualFile: VirtualFile?) {
    val path = if (virtualFile == null) "<null>" else virtualFile.path
    LOG.error(
            "Could not generate LightClass for $fqName declared in $path\n" +
            "System: ${SystemInfo.OS_NAME} ${SystemInfo.OS_VERSION} Java Runtime: ${SystemInfo.JAVA_RUNTIME_VERSION}",
            cause
    )
}

private val LOG = Logger.getInstance(LightClassDataProvider::class.java)