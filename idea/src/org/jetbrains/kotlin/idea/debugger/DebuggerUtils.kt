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

package org.jetbrains.kotlin.idea.debugger

import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtilRt
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.idea.KotlinFileTypeFactory
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.codeInsight.DescriptorToSourceUtilsIde
import org.jetbrains.kotlin.idea.resolve.ResolutionFacade
import org.jetbrains.kotlin.idea.stubindex.KotlinSourceFilterScope
import org.jetbrains.kotlin.idea.stubindex.PackageIndexUtil.findFilesWithExactPackage
import org.jetbrains.kotlin.idea.stubindex.StaticFacadeIndexUtil
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.CompositeBindingContext
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.inline.InlineUtil
import org.jetbrains.kotlin.resolve.jvm.JvmClassName
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedSimpleFunctionDescriptor
import org.jetbrains.kotlin.utils.addIfNotNull
import java.util.*

object DebuggerUtils {
    fun findSourceFileForClassIncludeLibrarySources(
            project: Project,
            scope: GlobalSearchScope,
            className: JvmClassName,
            fileName: String): KtFile? {
        return runReadAction {
            findSourceFileForClass(
                    project,
                    listOf(scope, KotlinSourceFilterScope.librarySources(GlobalSearchScope.allScope(project), project)),
                    className,
                    fileName)
        }
    }

    fun findSourceFileForClass(
            project: Project,
            scopes: List<GlobalSearchScope>,
            className: JvmClassName,
            fileName: String): KtFile? {
        val extension = FileUtilRt.getExtension(fileName)
        if (extension !in KotlinFileTypeFactory.KOTLIN_EXTENSIONS) return null
        if (DumbService.getInstance(project).isDumb) return null

        val filesWithExactName = scopes.findFirstNotEmpty { findFilesByNameInPackage(className, fileName, project, it) } ?: return null

        if (filesWithExactName.isEmpty()) return null

        if (filesWithExactName.size == 1) {
            return filesWithExactName.single()
        }

        // Static facade or inner class of such facade?
        val partFqName = className.fqNameForClassNameWithoutDollars
        val filesForPart = scopes.findFirstNotEmpty { StaticFacadeIndexUtil.findFilesForFilePart(partFqName, it, project) } ?: return null
        if (!filesForPart.isEmpty()) {
            for (file in filesForPart) {
                if (file.name == fileName) {
                    return file
                }
            }
            // Do not fall back to decompiled files (which have different name).
            return null
        }

        return filesWithExactName.first()
    }

    private fun <T, R> Collection<T>.findFirstNotEmpty(predicate: (T) -> Collection<R>): Collection<R>? {
        var result: Collection<R> = emptyList()
        for (e in this) {
            result = predicate(e)
            if (result.isNotEmpty()) break
        }
        return result
    }

    private fun findFilesByNameInPackage(className: JvmClassName, fileName: String, project: Project, searchScope: GlobalSearchScope)
            = findFilesWithExactPackage(className.packageFqName, searchScope, project).filter { it.name == fileName }

    fun analyzeInlinedFunctions(
            resolutionFacadeForFile: ResolutionFacade,
            file: KtFile,
            analyzeOnlyReifiedInlineFunctions: Boolean,
            bindingContext: BindingContext? = null
    ): Pair<BindingContext, List<KtFile>> {
        val analyzedElements = HashSet<KtElement>()
        val context = analyzeElementWithInline(
                resolutionFacadeForFile,
                file,
                1,
                analyzedElements,
                !analyzeOnlyReifiedInlineFunctions, bindingContext
        )

        //We processing another files just to annotate anonymous classes within their inline functions
        //Bytecode not produced for them cause of filtering via generateClassFilter
        val toProcess = LinkedHashSet<KtFile>()
        toProcess.add(file)

        for (collectedElement in analyzedElements) {
            val containingFile = collectedElement.containingKtFile
            toProcess.add(containingFile)
        }

        return Pair<BindingContext, List<KtFile>>(context, ArrayList(toProcess))
    }

    fun analyzeElementWithInline(function: KtNamedFunction, analyzeInlineFunctions: Boolean): Collection<KtElement> {
        val analyzedElements = HashSet<KtElement>()
        analyzeElementWithInline(function.getResolutionFacade(), function, 1, analyzedElements, !analyzeInlineFunctions)
        return analyzedElements
    }

    private fun analyzeElementWithInline(
            resolutionFacade: ResolutionFacade,
            element: KtElement,
            deep: Int,
            analyzedElements: MutableSet<KtElement>,
            analyzeInlineFunctions: Boolean,
            fullResolveContext: BindingContext? = null
    ): BindingContext {
        val project = element.project
        val inlineFunctions = HashSet<KtNamedFunction>()

        val innerContexts = ArrayList<BindingContext>()
        innerContexts.addIfNotNull(fullResolveContext)

        element.accept(object : KtTreeVisitorVoid() {
            override fun visitExpression(expression: KtExpression) {
                super.visitExpression(expression)

                val bindingContext = resolutionFacade.analyze(expression)
                innerContexts.add(bindingContext)

                val call = bindingContext.get(BindingContext.CALL, expression) ?: return

                val resolvedCall = bindingContext.get(BindingContext.RESOLVED_CALL, call)
                checkResolveCall(resolvedCall)
            }

            override fun visitDestructuringDeclaration(destructuringDeclaration: KtDestructuringDeclaration) {
                super.visitDestructuringDeclaration(destructuringDeclaration)

                val bindingContext = resolutionFacade.analyze(destructuringDeclaration)
                innerContexts.add(bindingContext)

                for (entry in destructuringDeclaration.entries) {
                    val resolvedCall = bindingContext.get(BindingContext.COMPONENT_RESOLVED_CALL, entry)
                    checkResolveCall(resolvedCall)
                }
            }

            override fun visitForExpression(expression: KtForExpression) {
                super.visitForExpression(expression)

                val bindingContext = resolutionFacade.analyze(expression)
                innerContexts.add(bindingContext)

                checkResolveCall(bindingContext.get(BindingContext.LOOP_RANGE_ITERATOR_RESOLVED_CALL, expression.loopRange))
                checkResolveCall(bindingContext.get(BindingContext.LOOP_RANGE_HAS_NEXT_RESOLVED_CALL, expression.loopRange))
                checkResolveCall(bindingContext.get(BindingContext.LOOP_RANGE_NEXT_RESOLVED_CALL, expression.loopRange))
            }

            private fun checkResolveCall(resolvedCall: ResolvedCall<*>?) {
                if (resolvedCall == null) return

                val descriptor = resolvedCall.resultingDescriptor
                if (descriptor is DeserializedSimpleFunctionDescriptor) return

                if (InlineUtil.isInline(descriptor) && (analyzeInlineFunctions || hasReifiedTypeParameters(descriptor))) {
                    val declaration = DescriptorToSourceUtilsIde.getAnyDeclaration(project, descriptor)
                    if (declaration != null && declaration is KtNamedFunction && !analyzedElements.contains(declaration)) {
                        inlineFunctions.add(declaration)
                    }
                }
            }
        })

        analyzedElements.add(element)

        if (!inlineFunctions.isEmpty() && deep < 10) {
            for (inlineFunction in inlineFunctions) {
                val body = inlineFunction.bodyExpression
                if (body != null) {
                    innerContexts.add(analyzeElementWithInline(resolutionFacade, inlineFunction, deep + 1, analyzedElements, analyzeInlineFunctions))
                }
            }

            analyzedElements.addAll(inlineFunctions)
        }

        return CompositeBindingContext.create(innerContexts)
    }

    private fun hasReifiedTypeParameters(descriptor: CallableDescriptor): Boolean {
        return descriptor.typeParameters.any() { it.isReified }
    }
}
