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
import com.sun.jdi.Location
import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.asJava.finder.JavaElementFinder
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
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
    @get:TestOnly
    var forceRanking = false

    fun findSourceFileForClassIncludeLibrarySources(
        project: Project,
        scope: GlobalSearchScope,
        className: JvmClassName,
        fileName: String,
        location: Location? = null
    ): KtFile? {
        return runReadAction {
            findSourceFileForClass(
                project,
                listOf(scope, KotlinSourceFilterScope.librarySources(GlobalSearchScope.allScope(project), project)),
                className,
                fileName,
                location
            )
        }
    }

    fun findSourceFileForClass(
        project: Project,
        scopes: List<GlobalSearchScope>,
        className: JvmClassName,
        fileName: String,
        location: Location?
    ): KtFile? {
        if (!isKotlinSourceFile(fileName)) return null
        if (DumbService.getInstance(project).isDumb) return null

        val partFqName = className.fqNameForClassNameWithoutDollars

        for (scope in scopes) {
            val files = findFilesByNameInPackage(className, fileName, project, scope)

            if (files.isEmpty()) {
                continue
            }

            if (files.size == 1 && !forceRanking || location == null) {
                return files.first()
            }

            StaticFacadeIndexUtil.findFilesForFilePart(partFqName, scope, project)
                .singleOrNull { it.name == fileName }
                ?.let { return it }

            return FileRankingCalculatorForIde.findMostAppropriateSource(files, location)
        }

        return null
    }

    private fun findFilesByNameInPackage(
        className: JvmClassName,
        fileName: String,
        project: Project,
        searchScope: GlobalSearchScope
    ): List<KtFile> {
        val files = findFilesWithExactPackage(className.packageFqName, searchScope, project).filter { it.name == fileName }
        return files.sortedWith(JavaElementFinder.byClasspathComparator(searchScope))
    }

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

    fun isKotlinSourceFile(fileName: String): Boolean {
        val extension = FileUtilRt.getExtension(fileName).toLowerCase()
        return extension in KotlinFileTypeFactory.KOTLIN_EXTENSIONS
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
        val declarationsWithBody = HashSet<KtDeclarationWithBody>()

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

                isAdditionalResolveNeededForDescriptor(descriptor)

                if (descriptor is PropertyDescriptor) {
                    for (accessor in descriptor.accessors) {
                        isAdditionalResolveNeededForDescriptor(accessor)
                    }
                }
            }

            private fun isAdditionalResolveNeededForDescriptor(descriptor: CallableDescriptor) {
                if (!(InlineUtil.isInline(descriptor) && (analyzeInlineFunctions || hasReifiedTypeParameters(descriptor)))) {
                    return
                }

                val declaration = DescriptorToSourceUtilsIde.getAnyDeclaration(project, descriptor)
                if (declaration != null && declaration is KtDeclarationWithBody && !analyzedElements.contains(declaration)) {
                    declarationsWithBody.add(declaration)
                    return
                }
            }
        })

        analyzedElements.add(element)

        if (!declarationsWithBody.isEmpty() && deep < 10) {
            for (inlineFunction in declarationsWithBody) {
                val body = inlineFunction.bodyExpression
                if (body != null) {
                    innerContexts.add(
                        analyzeElementWithInline(
                            resolutionFacade,
                            inlineFunction,
                            deep + 1,
                            analyzedElements,
                            analyzeInlineFunctions
                        )
                    )
                }
            }

            analyzedElements.addAll(declarationsWithBody)
        }

        return CompositeBindingContext.create(innerContexts)
    }

    private fun hasReifiedTypeParameters(descriptor: CallableDescriptor): Boolean {
        return descriptor.typeParameters.any { it.isReified }
    }
}
