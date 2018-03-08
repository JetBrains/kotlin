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

package org.jetbrains.kotlin.idea.conversion.copy

import com.intellij.psi.*
import com.intellij.psi.search.PsiShortNamesCache
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.caches.resolve.KotlinCacheService
import org.jetbrains.kotlin.descriptors.DeclarationDescriptorWithVisibility
import org.jetbrains.kotlin.idea.caches.project.getNullableModuleInfo
import org.jetbrains.kotlin.idea.caches.resolve.*
import org.jetbrains.kotlin.idea.caches.resolve.util.getJavaMemberDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.util.resolveToDescriptor
import org.jetbrains.kotlin.idea.core.isVisible
import org.jetbrains.kotlin.idea.imports.canBeReferencedViaImport
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.idea.util.ProjectRootsUtil
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.platform.JavaToKotlinClassMap
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtImportDirective
import org.jetbrains.kotlin.psi.psiUtil.referenceExpression
import org.jetbrains.kotlin.resolve.jvm.platform.JvmPlatform
import java.util.*


class PlainTextPasteImportResolver(val dataForConversion: DataForConversion, val targetFile: KtFile) {

    private val file = dataForConversion.file
    private val project = targetFile.project

    private val importList = file.importList!!
    private val psiElementFactory = PsiElementFactory.SERVICE.getInstance(project)

    private val bindingContext by lazy { targetFile.analyzeWithContent() }
    private val resolutionFacade = targetFile.getResolutionFacade()

    private val shortNameCache = PsiShortNamesCache.getInstance(project)
    private val scope = file.resolveScope

    private val failedToResolveReferenceNames = HashSet<String>()
    private var ambiguityInResolution = false
    private var couldNotResolve = false

    val addedImports = ArrayList<PsiImportStatementBase>()

    private fun canBeImported(descriptor: DeclarationDescriptorWithVisibility?): Boolean {
        return descriptor != null
               && descriptor.canBeReferencedViaImport()
               && descriptor.isVisible(targetFile, null, bindingContext, resolutionFacade)
    }

    private fun addImport(importStatement: PsiImportStatementBase, shouldAddToTarget: Boolean = false) {
        importList.add(importStatement)
        if (shouldAddToTarget)
            addedImports.add(importStatement)
    }

    fun addImportsFromTargetFile() {

        fun tryConvertKotlinImport(importDirective: KtImportDirective) {
            val importPath = importDirective.importPath
            val importedReference = importDirective.importedReference
            if (importPath != null && !importPath.hasAlias() && importedReference is KtDotQualifiedExpression) {
                val receiver = importedReference
                        .receiverExpression
                        .referenceExpression()
                        ?.mainReference
                        ?.resolve()
                val selector = importedReference
                        .selectorExpression
                        ?.referenceExpression()
                        ?.mainReference
                        ?.resolve()

                val isPackageReceiver = receiver is PsiPackage
                val isClassReceiver = receiver is PsiClass
                val isClassSelector = selector is PsiClass

                if (importPath.isAllUnder) {
                    if (isClassReceiver)
                        addImport(psiElementFactory.createImportStaticStatement(receiver as PsiClass, "*"))
                    else if (isPackageReceiver)
                        addImport(psiElementFactory.createImportStatementOnDemand((receiver as PsiPackage).qualifiedName))
                }
                else {
                    if (isClassSelector)
                        addImport(psiElementFactory.createImportStatement(selector as PsiClass))
                    else if (isClassReceiver)
                        addImport(psiElementFactory.createImportStaticStatement(receiver as PsiClass, importPath.importedName!!.asString()))
                }
            }
        }
        if (importList !in dataForConversion.elementsAndTexts.toList())
            runWriteAction {
                targetFile.importDirectives.forEach(::tryConvertKotlinImport)
            }
    }

    fun tryResolveReferences() {

        val elementsWithUnresolvedRef = PsiTreeUtil.collectElements(file) {
            it.reference != null
            && it.reference is PsiQualifiedReference
            && it.reference?.resolve() == null
        }

        fun tryResolveReference(reference: PsiQualifiedReference): Boolean {
            if (reference.resolve() != null) return true
            val referenceName = reference.referenceName ?: return false
            if (referenceName in failedToResolveReferenceNames) return false
            val classes = shortNameCache.getClassesByName(referenceName, scope)
                    .mapNotNull { psiClass ->
                        val containingFile = psiClass.containingFile
                        if (ProjectRootsUtil.isInProjectOrLibraryContent(containingFile)) {
                            val resolutionFacade = KotlinCacheService.getInstance(project).getResolutionFacadeByFile(
                                    containingFile, JvmPlatform
                            )
                            psiClass to psiClass.resolveToDescriptor(resolutionFacade)
                        }
                        else {
                            null
                        }
                    }
                    .filter { canBeImported(it.second) }

            classes.find { (_, descriptor) -> JavaToKotlinClassMap.mapPlatformClass(descriptor!!).isNotEmpty() }
                    ?.let { (psiClass, _) -> addImport(psiElementFactory.createImportStatement(psiClass)) }
            if (reference.resolve() != null) return true

            classes.singleOrNull()?.let { (psiClass, _) ->
                addImport(psiElementFactory.createImportStatement(psiClass), true)
            }

            if (reference.resolve() != null) return true
            else {
                if (classes.isNotEmpty()) {
                    ambiguityInResolution = true
                    return false
                }
            }

            val members = (shortNameCache.getMethodsByName(referenceName, scope).asList() +
                           shortNameCache.getFieldsByName(referenceName, scope).asList())
                    .map { it as PsiMember }
                    .filter { it.getNullableModuleInfo() != null }
                    .map { it to it.getJavaMemberDescriptor(resolutionFacade) as? DeclarationDescriptorWithVisibility }
                    .filter { canBeImported(it.second) }

            members.singleOrNull()?.let { (psiMember, _) ->
                addImport(psiElementFactory.createImportStaticStatement(psiMember.containingClass!!, psiMember.name!!), true)
            }

            if (reference.resolve() != null) return false
            else {
                if (members.isNotEmpty()) {
                    ambiguityInResolution = true
                }
                else {
                    couldNotResolve = true
                }
            }
            return false
        }

        runWriteAction {
            elementsWithUnresolvedRef.reversed().forEach {
                val reference = it.reference as PsiQualifiedReference
                if (!tryResolveReference(reference)) failedToResolveReferenceNames += reference.referenceName!!
            }
        }
    }
}