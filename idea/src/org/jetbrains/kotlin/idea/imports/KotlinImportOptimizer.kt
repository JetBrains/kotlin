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
import com.intellij.openapi.progress.ProgressIndicatorProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.idea.caches.resolve.ModuleSourceInfo
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.getNullableModuleInfo
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.core.formatter.KotlinCodeStyleSettings
import org.jetbrains.kotlin.idea.references.KtInvokeFunctionReference
import org.jetbrains.kotlin.idea.references.KtReference
import org.jetbrains.kotlin.idea.references.canBeResolvedViaImport
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.idea.util.application.runWriteAction
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
import java.util.*

class KotlinImportOptimizer() : ImportOptimizer {
    override fun supports(file: PsiFile?) = file is KtFile

    override fun processFile(file: PsiFile?) = Runnable {
        OptimizeProcess(file as KtFile).execute()
    }

    private class OptimizeProcess(private val file: KtFile) {
        fun execute() {
            if (file.getNullableModuleInfo() !is ModuleSourceInfo) return

            val oldImports = file.importDirectives
            if (oldImports.isEmpty()) return

            //TODO: keep existing imports? at least aliases (comments)

            val descriptorsToImport = collectDescriptorsToImport(file)

            val imports = prepareOptimizedImports(file, descriptorsToImport) ?: return

            runWriteAction { replaceImports(file, imports) }
        }
    }

    private class CollectUsedDescriptorsVisitor(file: KtFile) : KtVisitorVoid() {
        private val currentPackageName = file.packageFqName
        private val descriptorsToImport = HashSet<DeclarationDescriptor>()
        private val abstractRefs = ArrayList<OptimizedImportsBuilder.AbstractReference>()

        val data: OptimizedImportsBuilder.InputData
            get() = OptimizedImportsBuilder.InputData(descriptorsToImport, abstractRefs)

        override fun visitElement(element: PsiElement) {
            ProgressIndicatorProvider.checkCanceled()
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
                    if (importableDescriptor.name.asString() !in names) continue // resolved via alias

                    val importableFqName = target.importableFqName ?: continue
                    val parentFqName = importableFqName.parent()
                    if (target is PackageViewDescriptor && parentFqName == FqName.ROOT) continue // no need to import top-level packages
                    if (target !is PackageViewDescriptor && parentFqName == currentPackageName) continue

                    if (!reference.canBeResolvedViaImport(target)) continue

                    if (isAccessibleAsMember(importableDescriptor, element, bindingContext)) continue

                    descriptorsToImport.add(importableDescriptor)
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

                    is PropertyDescriptor ->
                        scope.findVariable(target.name, NoLookupLocation.FROM_IDE) { it == target } != null

                    is ClassDescriptor ->
                        scope.findClassifier(target.name, NoLookupLocation.FROM_IDE) == target

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
                        val additionalNames = (reference.element.calleeExpression as? KtNameReferenceExpression)?.mainReference?.resolvesByNames
                        if (additionalNames != null) {
                            return (resolvesByNames + additionalNames).map { Name.identifier(it) }
                        }
                    }
                    return resolvesByNames.map { Name.identifier(it) }
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
            val importList = file.importList!!
            val oldImports = importList.imports
            val psiFactory = KtPsiFactory(file.project)
            for (importPath in imports) {
                importList.addBefore(psiFactory.createImportDirective(importPath), oldImports.lastOrNull()) // insert into the middle to keep collapsed state
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
    }
}
