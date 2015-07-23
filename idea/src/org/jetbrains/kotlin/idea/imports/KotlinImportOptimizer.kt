/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

import com.google.common.collect.HashMultimap
import com.intellij.lang.ImportOptimizer
import com.intellij.openapi.progress.ProgressIndicatorProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.PackageViewDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.core.formatter.JetCodeStyleSettings
import org.jetbrains.kotlin.idea.references.JetReference
import org.jetbrains.kotlin.idea.util.ImportInsertHelper
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getReceiverExpression
import org.jetbrains.kotlin.psi.psiUtil.isAncestor
import org.jetbrains.kotlin.renderer.render
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.ImportPath
import org.jetbrains.kotlin.resolve.descriptorUtil.getImportableDescriptor
import org.jetbrains.kotlin.resolve.descriptorUtil.isExtension
import java.util.HashMap
import java.util.HashSet

public class KotlinImportOptimizer() : ImportOptimizer {

    override fun supports(file: PsiFile?) = file is JetFile

    override fun processFile(file: PsiFile?) = Runnable() {
        OptimizeProcess(file as JetFile).execute()
    }

    private class OptimizeProcess(private val file: JetFile) {
        public fun execute() {
            val oldImports = file.importDirectives
            if (oldImports.isEmpty()) return

            //TODO: keep existing imports? at least aliases (comments)

            val descriptorsToImport = collectDescriptorsToImport(file)

            val imports = prepareOptimizedImports(file, descriptorsToImport) ?: return

            runWriteAction { replaceImports(file, imports) }
        }
    }

    private class CollectUsedDescriptorsVisitor(val file: JetFile) : JetVisitorVoid() {
        private val _descriptors = HashSet<DeclarationDescriptor>()
        private val currentPackageName = file.packageFqName

        public val descriptors: Set<DeclarationDescriptor>
            get() = _descriptors

        override fun visitElement(element: PsiElement) {
            ProgressIndicatorProvider.checkCanceled()
            element.acceptChildren(this)
        }

        override fun visitImportList(importList: JetImportList) {
        }

        override fun visitPackageDirective(directive: JetPackageDirective) {
        }

        override fun visitJetElement(element: JetElement) {
            for (reference in element.references) {
                if (reference !is JetReference) continue

                val referencedName = (element as? JetNameReferenceExpression)?.getReferencedNameAsName() //TODO: other types of references

                val bindingContext = element.analyze()
                //class qualifiers that refer to companion objects should be considered (containing) class references
                val targets = bindingContext[BindingContext.SHORT_REFERENCE_TO_COMPANION_OBJECT, element as? JetReferenceExpression]?.let { listOf(it) }
                              ?: reference.resolveToDescriptors(bindingContext)
                for (target in targets) {
                    if (!target.canBeReferencedViaImport()) continue
                    val importableDescriptor = target.getImportableDescriptor()
                    val parentFqName = DescriptorUtils.getFqNameSafe(importableDescriptor).parent()
                    if (target is PackageViewDescriptor && parentFqName == FqName.ROOT) continue // no need to import top-level packages
                    if (target !is PackageViewDescriptor && parentFqName == currentPackageName) continue

                    if (!target.isExtension) { // for non-extension targets, count only non-qualified simple name usages
                        if (element !is JetNameReferenceExpression) continue
                        if (element.getIdentifier() == null) continue // skip 'this' etc
                        if (element.getReceiverExpression() != null) continue
                    }

                    if (referencedName != null && importableDescriptor.name != referencedName) continue // resolved via alias

                    if (isAccessibleAsMember(importableDescriptor, element)) continue

                    _descriptors.add(importableDescriptor)
                }
            }

            super.visitJetElement(element)
        }

        private fun isAccessibleAsMember(target: DeclarationDescriptor, place: JetElement): Boolean {
            val container = target.containingDeclaration
            if (container !is ClassDescriptor) return false
            val scope = if (DescriptorUtils.isCompanionObject(container))
                container.getContainingDeclaration() as? ClassDescriptor ?: return false
            else
                container
            val classBody = (DescriptorToSourceUtils.getSourceFromDescriptor(scope) as? JetClassOrObject)?.getBody()
            return classBody != null && classBody.containingFile == file && classBody.isAncestor(place)
        }
    }

    companion object {
        public fun collectDescriptorsToImport(file: JetFile): Set<DeclarationDescriptor> {
            val visitor = CollectUsedDescriptorsVisitor(file)
            file.accept(visitor)
            return visitor.descriptors
        }

        public fun prepareOptimizedImports(
                file: JetFile,
                descriptorsToImport: Collection<DeclarationDescriptor>
        ): List<ImportPath>? {
            val importInsertHelper = ImportInsertHelper.getInstance(file.project)
            val codeStyleSettings = JetCodeStyleSettings.getInstance(file.project)
            val aliasImports = buildAliasImportMap(file)

            val importsToGenerate = HashSet<ImportPath>()

            val descriptorsByPackages = HashMultimap.create<FqName, DeclarationDescriptor>()
            for (descriptor in descriptorsToImport) {
                val fqName = descriptor.importableFqNameSafe
                val parentFqName = fqName.parent()
                if (descriptor is PackageViewDescriptor || parentFqName.isRoot()) {
                    importsToGenerate.add(ImportPath(fqName, false))
                }
                else {
                    descriptorsByPackages.put(parentFqName, descriptor)
                }
            }

            val classNamesToCheck = HashSet<FqName>()

            fun isImportedByDefault(fqName: FqName) = importInsertHelper.isImportedWithDefault(ImportPath(fqName, false), file)

            for (packageName in descriptorsByPackages.keys()) {
                val descriptors = descriptorsByPackages[packageName]
                val fqNames = descriptors.map { it.importableFqNameSafe }.toSet()
                val explicitImports = fqNames.size() < codeStyleSettings.NAME_COUNT_TO_USE_STAR_IMPORT
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
                            classNamesToCheck.add(descriptor.importableFqNameSafe)
                        }
                    }

                    if (!fqNames.all(::isImportedByDefault)) {
                        importsToGenerate.add(ImportPath(packageName, true))
                    }
                }
            }

            // now check that there are no conflicts and all classes are really imported
            val fileWithImportsText = StringBuilder {
                append("package ").append(file.packageFqName.render()).append("\n")
                importsToGenerate.filter { it.isAllUnder() }.map { "import " + it.pathStr }.joinTo(this, "\n")
            }.toString()
            val fileWithImports = JetPsiFactory(file).createAnalyzableFile("Dummy.kt", fileWithImportsText, file)
            val scope = fileWithImports.getResolutionFacade().getFileTopLevelScope(fileWithImports)

            for (fqName in classNamesToCheck) {
                if (scope.getClassifier(fqName.shortName())?.importableFqNameSafe != fqName) {
                    // add explicit import if failed to import with * (or from current package)
                    importsToGenerate.add(ImportPath(fqName, false))

                    val packageName = fqName.parent()

                    for (descriptor in descriptorsByPackages[packageName].filter { it.importableFqNameSafe == fqName }) {
                        descriptorsByPackages.remove(packageName, descriptor)
                    }

                    if (descriptorsByPackages[packageName].isEmpty()) { // star import is not really needed
                        importsToGenerate.remove(ImportPath(packageName, true))
                    }
                }
            }

            //TODO: drop unused aliases?
            aliasImports.mapTo(importsToGenerate) { ImportPath(it.getValue(), false, it.getKey())}

            val sortedImportsToGenerate = importsToGenerate.sortBy(importInsertHelper.importSortComparator)

            // check if no changes to imports required
            val oldImports = file.importDirectives
            if (oldImports.size() == sortedImportsToGenerate.size() && oldImports.map { it.importPath } == sortedImportsToGenerate) return null

            return sortedImportsToGenerate
        }

        private fun buildAliasImportMap(file: JetFile): Map<Name, FqName> {
            val imports = file.importDirectives
            val aliasImports = HashMap<Name, FqName>()
            for (import in imports) {
                val path = import.importPath ?: continue
                val aliasName = path.alias
                if (aliasName != null) {
                    aliasImports.put(aliasName, path.fqnPart())
                }
            }
            return aliasImports
        }

        public fun replaceImports(file: JetFile, imports: List<ImportPath>) {
            val importList = file.importList!!
            val oldImports = importList.imports
            val psiFactory = JetPsiFactory(file.project)
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
