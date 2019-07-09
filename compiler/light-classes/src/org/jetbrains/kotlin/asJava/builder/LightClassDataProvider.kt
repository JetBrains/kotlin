/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.psi.impl.java.stubs.PsiJavaFileStub
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.analyzer.KotlinModificationTrackerService
import org.jetbrains.kotlin.asJava.KotlinAsJavaSupport
import org.jetbrains.kotlin.asJava.LightClassGenerationSupport
import org.jetbrains.kotlin.asJava.classes.getOutermostClassOrObject
import org.jetbrains.kotlin.codegen.CompilationErrorHandler
import org.jetbrains.kotlin.codegen.MemberCodegen
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.fileClasses.JvmFileClassUtil
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.isAncestor
import org.jetbrains.org.objectweb.asm.Type

class LightClassDataProviderForClassOrObject(
        private val classOrObject: KtClassOrObject
) : CachedValueProvider<LightClassDataHolder.ForClass> {

    private fun computeLightClassData(): LightClassDataHolder.ForClass {
        val file = classOrObject.containingKtFile
        val packageFqName = file.packageFqName
        return LightClassGenerationSupport.getInstance(classOrObject.project).createDataHolderForClass(classOrObject) {
            constructionContext ->
            buildLightClass(packageFqName, listOf(file), ClassFilterForClassOrObject(classOrObject), constructionContext) {
                state, files ->
                val packageCodegen = state.factory.forPackage(packageFqName, files)
                val packagePartType = Type.getObjectType(JvmFileClassUtil.getFileClassInternalName(file))
                val context = state.rootContext.intoPackagePart(packageCodegen.packageFragment, packagePartType, file)
                MemberCodegen.genClassOrObject(context, getOutermostClassOrObject(classOrObject), state, null)
                state.factory.done()
            }
        }
    }

    override fun compute(): CachedValueProvider.Result<LightClassDataHolder.ForClass>? {
        return CachedValueProvider.Result.create(
                computeLightClassData(),
                if (classOrObject.isLocal()) {
                    KotlinModificationTrackerService.getInstance(classOrObject.project).modificationTracker
                } else {
                    KotlinModificationTrackerService.getInstance(classOrObject.project).outOfBlockModificationTracker
                }
        )
    }

    override fun toString(): String {
        return this::class.java.name + " for " + classOrObject.name
    }
}

sealed class LightClassDataProviderForFileFacade constructor(
        protected val project: Project, protected val facadeFqName: FqName
) : CachedValueProvider<LightClassDataHolder.ForFacade> {
    abstract fun findFiles(): Collection<KtFile>

    private fun computeLightClassData(files: Collection<KtFile>): LightClassDataHolder.ForFacade {
        return LightClassGenerationSupport.getInstance(project).createDataHolderForFacade(files) {
            constructionContext ->
            buildLightClass(facadeFqName.parent(), files, ClassFilterForFacade, constructionContext) generate@ {
                state, files ->
                val representativeFile = files.first()
                val fileClassInfo = JvmFileClassUtil.getFileClassInfoNoResolve(representativeFile)
                if (!fileClassInfo.withJvmMultifileClass) {
                    val codegen = state.factory.forPackage(representativeFile.packageFqName, files)
                    codegen.generate(CompilationErrorHandler.THROW_EXCEPTION)
                    state.factory.done()
                    return@generate
                }

                val codegen = state.factory.forMultifileClass(facadeFqName, files)
                codegen.generate(CompilationErrorHandler.THROW_EXCEPTION)
                state.factory.done()
            }
        }
    }

    override fun compute(): CachedValueProvider.Result<LightClassDataHolder.ForFacade>? {
        val files = findFiles()
        if (files.isEmpty()) return null

        return CachedValueProvider.Result.create(
                computeLightClassData(files),
                KotlinModificationTrackerService.getInstance(project).outOfBlockModificationTracker
        )
    }

    override fun toString(): String {
        return this::class.java.name + " for $facadeFqName"
    }

    // create delegate by relevant files in project source using LightClassGenerationSupport
    class ByProjectSource(
            project: Project,
            facadeFqName: FqName,
            private val searchScope: GlobalSearchScope
    ) : LightClassDataProviderForFileFacade(project, facadeFqName) {
        override fun findFiles() = KotlinAsJavaSupport.getInstance(project).findFilesForFacade(facadeFqName, searchScope)
    }

    // create delegate by single file
    class ByFile(
            project: Project,
            facadeFqName: FqName,
            private val file: KtFile
    ) : LightClassDataProviderForFileFacade(project, facadeFqName) {
        override fun findFiles() = listOf(file)
    }
}


class LightClassDataProviderForScript(private val script: KtScript) : CachedValueProvider<LightClassDataHolder.ForScript> {
    private fun computeLightClassData(): LightClassDataHolder.ForScript {
        return LightClassGenerationSupport.getInstance(script.project).createDataHolderForScript(script) {
            constructionContext ->
            buildLightClass(
                    script.fqName.parent(),
                    listOf(script.containingKtFile),
                    ClassFilterForScript(script),
                    constructionContext
            ) generate@ {
                state, files ->
                val scriptFile = files.first()
                val codegen = state.factory.forPackage(scriptFile.packageFqName, files)
                codegen.generate(CompilationErrorHandler.THROW_EXCEPTION)
                state.factory.done()
            }
        }
    }

    override fun compute(): CachedValueProvider.Result<LightClassDataHolder.ForScript>? =
            CachedValueProvider.Result.create(
                computeLightClassData(),
                KotlinModificationTrackerService.getInstance(script.project).outOfBlockModificationTracker
            )

    override fun toString(): String = this::class.java.name + " for ${script.fqName}"
}

interface StubComputationTracker {
    fun onStubComputed(javaFileStub: PsiJavaFileStub, context: LightClassConstructionContext)
}


private class ClassFilterForClassOrObject(private val classOrObject: KtClassOrObject) : GenerationState.GenerateClassFilter() {

    override fun shouldGeneratePackagePart(ktFile: KtFile) = true
    override fun shouldAnnotateClass(processingClassOrObject: KtClassOrObject) = shouldGenerateClass(processingClassOrObject)

    override fun shouldGenerateClassMembers(processingClassOrObject: KtClassOrObject): Boolean {
        if (classOrObject === processingClassOrObject) return true

        // process all children
        if (classOrObject.isAncestor(processingClassOrObject, true)) {
            return true
        }

        // Local classes should be process by CodegenAnnotatingVisitor to
        // decide what class they should be placed in.
        //
        // Example:
        // class A
        // fun foo() {
        //     trait Z: A {}
        //     fun bar() {
        //         class <caret>O2: Z {}
        //     }
        // }
        // TODO: current method will process local classes in irrelevant declarations, it should be fixed.
        // We generate all enclosing classes

        if (classOrObject.isLocal && processingClassOrObject.isLocal) {
            val commonParent = PsiTreeUtil.findCommonParent(classOrObject, processingClassOrObject)
            return commonParent != null && commonParent !is PsiFile
        }

        return false
    }

    override fun shouldGenerateClass(processingClassOrObject: KtClassOrObject)
            // generate outer classes but not their members
            = shouldGenerateClassMembers(processingClassOrObject) || processingClassOrObject.isAncestor(classOrObject, true)

    override fun shouldGenerateScript(script: KtScript) = PsiTreeUtil.isAncestor(script, classOrObject, false)
    override fun shouldGenerateCodeFragment(script: KtCodeFragment) = false
}

object ClassFilterForFacade : GenerationState.GenerateClassFilter() {
    override fun shouldAnnotateClass(processingClassOrObject: KtClassOrObject) = shouldGenerateClass(processingClassOrObject)
    override fun shouldGenerateClass(processingClassOrObject: KtClassOrObject) = KtPsiUtil.isLocal(processingClassOrObject)
    override fun shouldGeneratePackagePart(ktFile: KtFile) = true
    override fun shouldGenerateScript(script: KtScript) = false
    override fun shouldGenerateCodeFragment(script: KtCodeFragment) = false
}

private class ClassFilterForScript(val script: KtScript) : GenerationState.GenerateClassFilter() {
    override fun shouldAnnotateClass(processingClassOrObject: KtClassOrObject): Boolean =
            shouldGenerateClass(processingClassOrObject)

    override fun shouldGenerateClass(processingClassOrObject: KtClassOrObject): Boolean =
            processingClassOrObject.isAncestor(script, true)

    override fun shouldGenerateClassMembers(processingClassOrObject: KtClassOrObject): Boolean = true

    override fun shouldGeneratePackagePart(ktFile: KtFile): Boolean = script.containingKtFile === ktFile

    override fun shouldGenerateScript(script: KtScript): Boolean = this.script === script
    override fun shouldGenerateCodeFragment(script: KtCodeFragment) = false
}
