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

package org.jetbrains.kotlin.idea.imports

import com.intellij.lang.ImportOptimizer
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressIndicatorProvider
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.idea.caches.project.ModuleSourceInfo
import org.jetbrains.kotlin.idea.caches.project.ScriptModuleInfo
import org.jetbrains.kotlin.idea.caches.project.getNullableModuleInfo
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.core.formatter.KotlinCodeStyleSettings
import org.jetbrains.kotlin.idea.references.KtInvokeFunctionReference
import org.jetbrains.kotlin.idea.references.KtReference
import org.jetbrains.kotlin.idea.references.canBeResolvedViaImport
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.idea.util.getResolutionScope
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.ImportPath
import org.jetbrains.kotlin.resolve.descriptorUtil.getImportableDescriptor
import org.jetbrains.kotlin.resolve.scopes.HierarchicalScope
import org.jetbrains.kotlin.resolve.scopes.utils.*

class KotlinImportOptimizer : ImportOptimizer {
    override fun supports(file: PsiFile) = file is KtFile

    override fun processFile(file: PsiFile): ImportOptimizer.CollectingInfoRunnable {
        val ktFile = (file as? KtFile) ?: return DO_NOTHING
        val (add, remove, imports) = prepareImports(ktFile) ?: return DO_NOTHING

        return object : ImportOptimizer.CollectingInfoRunnable {
            override fun getUserNotificationInfo(): String = if (remove == 0) "Rearranged imports"
            else "Removed $remove ${StringUtil.pluralize("import", remove)}" +
                    if (add > 0) ", added $add ${StringUtil.pluralize("import", add)}" else ""

            override fun run() = replaceImports(ktFile, imports)
        }
    }

    // The same as com.intellij.pom.core.impl.PomModelImpl.isDocumentUncommitted
    // Which is checked in com.intellij.pom.core.impl.PomModelImpl.startTransaction
    private val KtFile.isDocumentUncommitted: Boolean
        get() {
            val documentManager = PsiDocumentManager.getInstance(project)
            val cachedDocument = documentManager.getCachedDocument(this)
            return cachedDocument != null && documentManager.isUncommited(cachedDocument)
        }

    private fun prepareImports(file: KtFile): OptimizeInformation? {
        ApplicationManager.getApplication().assertReadAccessAllowed()

        // Optimize imports may be called after command
        // And document can be uncommitted after running that command
        // In that case we will get ISE: Attempt to modify PSI for non-committed Document!
        if (file.isDocumentUncommitted) return null

        val moduleInfo = file.getNullableModuleInfo()
        if (moduleInfo !is ModuleSourceInfo && moduleInfo !is ScriptModuleInfo) return null

        val oldImports = file.importDirectives
        if (oldImports.isEmpty()) return null

        //TODO: keep existing imports? at least aliases (comments)

        ProgressIndicatorProvider.getInstance().progressIndicator?.text = "Collect unused imports for ${file.name}"

        val descriptorsToImport = collectDescriptorsToImport(file)

        val imports = prepareOptimizedImports(file, descriptorsToImport) ?: return null
        val intersect = imports.intersect(oldImports.map { it.importPath })
        return OptimizeInformation(
            add = imports.size - intersect.size,
            remove = oldImports.size - intersect.size,
            imports = imports
        )
    }

    private data class OptimizeInformation(val add: Int, val remove: Int, val imports: List<ImportPath>)

    private class CollectUsedDescriptorsVisitor(file: KtFile) : KtVisitorVoid() {
        private val elementsSize: Int

        init {
            var size = 0
            file.accept(object : KtVisitorVoid() {
                override fun visitElement(element: PsiElement) {
                    size += 1
                    element.acceptChildren(this)
                }
            })
            elementsSize = size
        }

        private var elementProgress: Int = 0
        private val currentPackageName = file.packageFqName
        private val aliases: Map<FqName, List<Name>> = file.importDirectives
            .asSequence()
            .filter { !it.isAllUnder && it.alias != null }
            .mapNotNull { it.importPath }
            .groupBy(keySelector = { it.fqName }, valueTransform = { it.importedName as Name })

        private val descriptorsToImport = LinkedHashSet<DeclarationDescriptor>()
        private val namesToImport = LinkedHashMap<FqName, HashSet<Name>>()
        private val abstractRefs = ArrayList<OptimizedImportsBuilder.AbstractReference>()

        val data: OptimizedImportsBuilder.InputData
            get() = OptimizedImportsBuilder.InputData(descriptorsToImport, namesToImport, abstractRefs)

        override fun visitElement(element: PsiElement) {
            ProgressIndicatorProvider.checkCanceled()
            elementProgress += 1
            ProgressIndicatorProvider.getInstance().progressIndicator?.fraction = elementProgress / elementsSize.toDouble()

            element.acceptChildren(this)
        }

        override fun visitImportList(importList: KtImportList) {
        }

        override fun visitPackageDirective(directive: KtPackageDirective) {
        }

        override fun visitKtElement(element: KtElement) {
            for (reference in element.references) {
                if (reference !is KtReference) continue
                abstractRefs.add(AbstractReferenceImpl(reference))

                val names = reference.resolvesByNames

                val bindingContext = element.analyze()
                val targets = reference.targets(bindingContext)
                for (target in targets) {
                    val importableDescriptor = target.getImportableDescriptor()

                    val importableFqName = target.importableFqName ?: continue
                    val parentFqName = importableFqName.parent()
                    if (target is PackageViewDescriptor && parentFqName == FqName.ROOT) continue // no need to import top-level packages

                    if (target !is PackageViewDescriptor && parentFqName == currentPackageName && (importableFqName !in aliases)) continue

                    if (!reference.canBeResolvedViaImport(target, bindingContext)) continue

                    if (isAccessibleAsMember(importableDescriptor, element, bindingContext)) continue

                    val descriptorNames = (aliases[importableFqName].orEmpty() + importableFqName.shortName()).intersect(names)
                    namesToImport.getOrPut(importableFqName) { LinkedHashSet() } += descriptorNames
                    descriptorsToImport += importableDescriptor
                }
            }

            super.visitKtElement(element)
        }

        private fun isAccessibleAsMember(target: DeclarationDescriptor, place: KtElement, bindingContext: BindingContext): Boolean {
            if (target.containingDeclaration !is ClassDescriptor) return false

            fun isInScope(scope: HierarchicalScope): Boolean {
                return when (target) {
                    is FunctionDescriptor ->
                        scope.findFunction(target.name, NoLookupLocation.FROM_IDE) { it == target } != null
                                && bindingContext[BindingContext.DEPRECATED_SHORT_NAME_ACCESS, place] != true

                    is PropertyDescriptor ->
                        scope.findVariable(target.name, NoLookupLocation.FROM_IDE) { it == target } != null
                                && bindingContext[BindingContext.DEPRECATED_SHORT_NAME_ACCESS, place] != true

                    is ClassDescriptor ->
                        scope.findClassifier(target.name, NoLookupLocation.FROM_IDE) == target
                                && bindingContext[BindingContext.DEPRECATED_SHORT_NAME_ACCESS, place] != true

                    else -> false
                }
            }

            val resolutionScope = place.getResolutionScope(bindingContext, place.getResolutionFacade())
            val noImportsScope = resolutionScope.replaceImportingScopes(null)

            if (isInScope(noImportsScope)) return true
            if (target !is ClassDescriptor) { // classes not accessible through receivers, only their constructors
                if (resolutionScope.getImplicitReceiversHierarchy().any { isInScope(it.type.memberScope.memberScopeAsImportingScope()) }) return true
            }
            return false
        }

        private class AbstractReferenceImpl(private val reference: KtReference) : OptimizedImportsBuilder.AbstractReference {
            override val element: KtElement
                get() = reference.element

            override val dependsOnNames: Collection<Name>
                get() {
                    val resolvesByNames = reference.resolvesByNames
                    if (reference is KtInvokeFunctionReference) {
                        val additionalNames = (reference.element.calleeExpression as? KtNameReferenceExpression)
                            ?.mainReference?.resolvesByNames
                        if (additionalNames != null) {
                            return resolvesByNames + additionalNames
                        }
                    }
                    return resolvesByNames
                }

            override fun resolve(bindingContext: BindingContext) = reference.resolveToDescriptors(bindingContext)

            override fun toString() = reference.toString()
        }
    }

    companion object {
        fun collectDescriptorsToImport(file: KtFile): OptimizedImportsBuilder.InputData {
            val visitor = CollectUsedDescriptorsVisitor(file)
            file.accept(visitor)
            return visitor.data
        }

        fun prepareOptimizedImports(file: KtFile, data: OptimizedImportsBuilder.InputData): List<ImportPath>? {
            val settings = KotlinCodeStyleSettings.getInstance(file.project)
            val options = OptimizedImportsBuilder.Options(
                settings.NAME_COUNT_TO_USE_STAR_IMPORT,
                settings.NAME_COUNT_TO_USE_STAR_IMPORT_FOR_MEMBERS,
                isInPackagesToUseStarImport = { fqName -> fqName.asString() in settings.PACKAGES_TO_USE_STAR_IMPORTS })
            return OptimizedImportsBuilder(file, data, options).buildOptimizedImports()
        }

        fun replaceImports(file: KtFile, imports: List<ImportPath>) {
            val importList = file.importList ?: return
            val oldImports = importList.imports
            val psiFactory = KtPsiFactory(file.project)
            for (importPath in imports) {
                importList.addBefore(
                    psiFactory.createImportDirective(importPath),
                    oldImports.lastOrNull()
                ) // insert into the middle to keep collapsed state
            }

            // remove old imports after adding new ones to keep imports folding state
            for (import in oldImports) {
                import.delete()
            }
        }

        private fun KtReference.targets(bindingContext: BindingContext): Collection<DeclarationDescriptor> {
            //class qualifiers that refer to companion objects should be considered (containing) class references
            return bindingContext[BindingContext.SHORT_REFERENCE_TO_COMPANION_OBJECT, element as? KtReferenceExpression]?.let { listOf(it) }
                ?: resolveToDescriptors(bindingContext)
        }

        private val DO_NOTHING = object : ImportOptimizer.CollectingInfoRunnable {
            override fun run() = Unit

            override fun getUserNotificationInfo() = "Unused imports not found"
        }
    }
}
