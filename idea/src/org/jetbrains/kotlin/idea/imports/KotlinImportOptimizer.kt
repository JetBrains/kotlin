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
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressIndicatorProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.PackageViewDescriptor
import org.jetbrains.kotlin.idea.formatter.JetCodeStyleSettings
import org.jetbrains.kotlin.idea.references.JetReference
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.idea.util.ImportInsertHelper
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getReceiverExpression
import org.jetbrains.kotlin.psi.psiUtil.isAncestor
import org.jetbrains.kotlin.resolve.descriptorUtil.getImportableDescriptor
import org.jetbrains.kotlin.resolve.descriptorUtil.isExtension
import java.util.ArrayList
import java.util.HashMap
import java.util.HashSet
import org.jetbrains.kotlin.idea.caches.resolve.*
import org.jetbrains.kotlin.resolve.*

public class KotlinImportOptimizer() : ImportOptimizer {

    override fun supports(file: PsiFile?) = file is JetFile

    override fun processFile(file: PsiFile?) = Runnable() {
        OptimizeProcess(file as JetFile).execute()
    }

    private class OptimizeProcess(val file: JetFile) {
        private val codeStyleSettings = JetCodeStyleSettings.getInstance(file.getProject())
        private val aliasImports: Map<Name, FqName>

        ;{
            val imports = file.getImportDirectives()
            val aliasImports = HashMap<Name, FqName>()
            for (import in imports) {
                val path = import.getImportPath() ?: continue
                val aliasName = path.getAlias()
                if (aliasName != null) {
                    aliasImports.put(aliasName, path.fqnPart())
                }
            }
            this.aliasImports = aliasImports
        }

        public fun execute() {
            val oldImports = file.getImportDirectives()
            if (oldImports.isEmpty()) return

            //TODO: keep existing imports? at least aliases (comments)

            val importInsertHelper = ImportInsertHelper.getInstance(file.getProject())
            val currentPackageName = file.getPackageFqName()

            val descriptorsToImport = detectDescriptorsToImport()

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
                val explicitImports = packageName != currentPackageName && fqNames.size() < codeStyleSettings.NAME_COUNT_TO_USE_STAR_IMPORT
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

                    if (packageName != currentPackageName && !fqNames.all(::isImportedByDefault)) {
                        importsToGenerate.add(ImportPath(packageName, true))
                    }
                }
            }

            // now check that there are no conflicts and all classes are really imported
            val fileWithImportsText = StringBuilder {
                append("package ").append(IdeDescriptorRenderers.SOURCE_CODE.renderFqName(currentPackageName)).append("\n")
                importsToGenerate.filter { it.isAllUnder() }.map { "import " + it.getPathStr() }.joinTo(this, "\n")
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
            if (oldImports.size() == sortedImportsToGenerate.size() && oldImports.map { it.getImportPath() } == sortedImportsToGenerate) return

            ApplicationManager.getApplication()!!.runWriteAction(Runnable {
                val importList = file.getImportList()!!
                val psiFactory = JetPsiFactory(file.getProject())
                for (importPath in sortedImportsToGenerate) {
                    importList.addBefore(psiFactory.createImportDirective(importPath), oldImports.lastOrNull()) // insert into the middle to keep collapsed state
                }

                // remove old imports after adding new ones to keep imports folding state
                for (import in oldImports) {
                    import.delete()
                }
            })
        }

        private fun detectDescriptorsToImport(): Set<DeclarationDescriptor> {
            val usedDescriptors = HashSet<DeclarationDescriptor>()
            file.accept(object : JetVisitorVoid() {
                override fun visitElement(element: PsiElement) {
                    ProgressIndicatorProvider.checkCanceled()
                    element.acceptChildren(this)
                }

                override fun visitImportList(importList: JetImportList) {
                }

                override fun visitPackageDirective(directive: JetPackageDirective) {
                }


                private fun JetElement.classForDefaultObjectReference(): ClassDescriptor? {
                    return analyze()[BindingContext.SHORT_REFERENCE_TO_DEFAULT_OBJECT, this as? JetReferenceExpression]
                }

                override fun visitJetElement(element: JetElement) {
                    val reference = element.getReference()
                    if (reference is JetReference) {
                        val referencedName = (element as? JetNameReferenceExpression)?.getReferencedNameAsName() //TODO: other types of references

                        //class qualifiers that refer to default objects should be considered (containing) class references
                        val targets = element.classForDefaultObjectReference()?.let { listOf(it) }
                                      ?: reference.resolveToDescriptors()
                        for (target in targets) {
                            if (!target.canBeReferencedViaImport()) continue
                            if (target is PackageViewDescriptor && target.getFqName().parent() == FqName.ROOT) continue // no need to import top-level packages

                            if (!target.isExtension) { // for non-extension targets, count only non-qualified simple name usages
                                if (element !is JetNameReferenceExpression) continue
                                if (element.getIdentifier() == null) continue // skip 'this' etc
                                if (element.getReceiverExpression() != null) continue
                            }

                            val importableDescriptor = target.getImportableDescriptor()
                            if (referencedName != null && importableDescriptor.getName() != referencedName) continue // resolved via alias

                            if (isAccessibleAsMember(importableDescriptor, element)) continue

                            usedDescriptors.add(importableDescriptor)
                        }
                    }

                    super.visitJetElement(element)
                }
            })
            return usedDescriptors
        }

        private fun isAccessibleAsMember(target: DeclarationDescriptor, place: JetElement): Boolean {
            val container = target.getContainingDeclaration()
            if (container !is ClassDescriptor) return false
            val scope = if (DescriptorUtils.isDefaultObject(container))
                container.getContainingDeclaration() as? ClassDescriptor ?: return false
            else
                container
            val classBody = (DescriptorToSourceUtils.classDescriptorToDeclaration(scope) as? JetClassOrObject)?.getBody()
            return classBody != null && classBody.getContainingFile() == file && classBody.isAncestor(place)
        }
    }
}
