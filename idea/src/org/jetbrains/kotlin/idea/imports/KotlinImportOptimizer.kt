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
import org.jetbrains.kotlin.idea.references.KtReference
import org.jetbrains.kotlin.idea.references.canBeResolvedViaImport
import org.jetbrains.kotlin.idea.util.ImportInsertHelper
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.idea.util.getFileResolutionScope
import org.jetbrains.kotlin.idea.util.getResolutionScope
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.renderer.render
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.ImportPath
import org.jetbrains.kotlin.resolve.descriptorUtil.getImportableDescriptor
import org.jetbrains.kotlin.resolve.scopes.HierarchicalScope
import org.jetbrains.kotlin.resolve.scopes.utils.*
import java.util.*

class KotlinImportOptimizer() : ImportOptimizer {

    override fun supports(file: PsiFile?) = file is KtFile

    override fun processFile(file: PsiFile?) = Runnable() {
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

    private class CollectUsedDescriptorsVisitor(val file: KtFile) : KtVisitorVoid() {
        private val _descriptors = HashSet<DeclarationDescriptor>()
        private val currentPackageName = file.packageFqName

        val descriptors: Set<DeclarationDescriptor>
            get() = _descriptors

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

                val referencedName = (element as? KtNameReferenceExpression)?.getReferencedNameAsName() //TODO: other types of references

                val bindingContext = element.analyze()
                //class qualifiers that refer to companion objects should be considered (containing) class references
                val targets = bindingContext[BindingContext.SHORT_REFERENCE_TO_COMPANION_OBJECT, element as? KtReferenceExpression]?.let { listOf(it) }
                              ?: reference.resolveToDescriptors(bindingContext)
                for (target in targets) {
                    val importableFqName = target.importableFqName ?: continue
                    val parentFqName = importableFqName.parent()
                    if (target is PackageViewDescriptor && parentFqName == FqName.ROOT) continue // no need to import top-level packages
                    if (target !is PackageViewDescriptor && parentFqName == currentPackageName) continue

                    if (!reference.canBeResolvedViaImport(target)) continue

                    val importableDescriptor = target.getImportableDescriptor()

                    if (referencedName != null && importableDescriptor.name != referencedName) continue // resolved via alias

                    if (isAccessibleAsMember(importableDescriptor, element, bindingContext)) continue

                    _descriptors.add(importableDescriptor)
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
    }

    companion object {
        fun collectDescriptorsToImport(file: KtFile): Set<DeclarationDescriptor> {
            val visitor = CollectUsedDescriptorsVisitor(file)
            file.accept(visitor)
            return visitor.descriptors
        }

        fun prepareOptimizedImports(file: KtFile,
                                    descriptorsToImport: Collection<DeclarationDescriptor>): List<ImportPath>? {
            val settings = KotlinCodeStyleSettings.getInstance(file.project)
            return prepareOptimizedImports(
                    file,
                    descriptorsToImport,
                    settings.NAME_COUNT_TO_USE_STAR_IMPORT,
                    settings.NAME_COUNT_TO_USE_STAR_IMPORT_FOR_MEMBERS) { fqName ->
                fqName.asString() in settings.PACKAGES_TO_USE_STAR_IMPORTS
            }
        }

        fun prepareOptimizedImports(
                file: KtFile,
                descriptorsToImport: Collection<DeclarationDescriptor>,
                nameCountToUseStarImport: Int,
                nameCountToUseStarImportForMembers: Int,
                isInPackagesToUseStarImport: (FqName) -> Boolean
        ): List<ImportPath>? {
            val importInsertHelper = ImportInsertHelper.getInstance(file.project)
            val aliasImports = buildAliasImportMap(file)

            val importsToGenerate = HashSet<ImportPath>()

            val descriptorsByParentFqName = hashMapOf<FqName, MutableSet<DeclarationDescriptor>>()
            for (descriptor in descriptorsToImport) {
                val fqName = descriptor.importableFqName!!
                val container = descriptor.containingDeclaration
                val parentFqName = fqName.parent()
                val canUseStarImport = when {
                    parentFqName.isRoot -> false
                    (container as? ClassDescriptor)?.kind == ClassKind.OBJECT -> false
                    else -> true
                }
                if (canUseStarImport) {
                    val descriptors = descriptorsByParentFqName.getOrPut(parentFqName) { hashSetOf() }
                    descriptors.add(descriptor)
                }
                else {
                    importsToGenerate.add(ImportPath(fqName, false))
                }
            }

            val classNamesToCheck = HashSet<FqName>()

            fun isImportedByDefault(fqName: FqName) = importInsertHelper.isImportedWithDefault(ImportPath(fqName, false), file)

            for (parentFqName in descriptorsByParentFqName.keys) {
                val descriptors = descriptorsByParentFqName[parentFqName]!!
                val fqNames = descriptors.map { it.importableFqName!! }.toSet()
                val isMember = descriptors.first().containingDeclaration is ClassDescriptor
                val nameCountToUseStar = if (isMember)
                    nameCountToUseStarImportForMembers
                else
                    nameCountToUseStarImport
                val explicitImports = fqNames.size < nameCountToUseStar && !isInPackagesToUseStarImport(parentFqName)
                if (explicitImports) {
                    for (fqName in fqNames) {
                        if (!isImportedByDefault(fqName)) {
                            importsToGenerate.add(ImportPath(fqName, false))
                        }
                    }
                }
                else {
                    for (descriptor in descriptors) {
                        if (descriptor is ClassDescriptor) {
                            classNamesToCheck.add(descriptor.importableFqName!!)
                        }
                    }

                    if (!fqNames.all(::isImportedByDefault)) {
                        importsToGenerate.add(ImportPath(parentFqName, true))
                    }
                }
            }

            // now check that there are no conflicts and all classes are really imported
            val fileWithImportsText = buildString {
                append("package ").append(file.packageFqName.toUnsafe().render()).append("\n")
                importsToGenerate.filter { it.isAllUnder }.map { "import " + it.pathStr }.joinTo(this, "\n")
            }
            val fileWithImports = KtPsiFactory(file).createAnalyzableFile("Dummy.kt", fileWithImportsText, file)
            val scope = fileWithImports.getResolutionFacade().getFileResolutionScope(fileWithImports)

            for (fqName in classNamesToCheck) {
                if (scope.findClassifier(fqName.shortName(), NoLookupLocation.FROM_IDE)?.importableFqName != fqName) {
                    // add explicit import if failed to import with * (or from current package)
                    importsToGenerate.add(ImportPath(fqName, false))

                    val parentFqName = fqName.parent()

                    val parentDescriptors = descriptorsByParentFqName[parentFqName]!!
                    for (descriptor in parentDescriptors.filter { it.importableFqName == fqName }) {
                        parentDescriptors.remove(descriptor)
                    }

                    if (parentDescriptors.isEmpty()) { // star import is not really needed
                        importsToGenerate.remove(ImportPath(parentFqName, true))
                    }
                }
            }

            //TODO: drop unused aliases?
            aliasImports.mapTo(importsToGenerate) { ImportPath(it.value, false, it.key) }

            val sortedImportsToGenerate = importsToGenerate.sortedWith(importInsertHelper.importSortComparator)

            // check if no changes to imports required
            val oldImports = file.importDirectives
            if (oldImports.size == sortedImportsToGenerate.size && oldImports.map { it.importPath } == sortedImportsToGenerate) return null

            return sortedImportsToGenerate
        }

        private fun buildAliasImportMap(file: KtFile): Map<Name, FqName> {
            val imports = file.importDirectives
            val aliasImports = HashMap<Name, FqName>()
            for (import in imports) {
                val path = import.importPath ?: continue
                val aliasName = path.alias
                if (aliasName != null && aliasName != path.fqnPart().shortName() /* we do not keep trivial aliases */) {
                    aliasImports.put(aliasName, path.fqnPart())
                }
            }
            return aliasImports
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
    }
}
