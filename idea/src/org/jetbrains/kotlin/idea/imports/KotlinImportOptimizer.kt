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

import com.intellij.lang.ImportOptimizer
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressIndicatorProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlin.idea.references.JetReference
import java.util.*
import org.jetbrains.kotlin.resolve.descriptorUtil.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.idea.formatter.JetCodeStyleSettings
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.psi.psiUtil.*
import org.jetbrains.kotlin.idea.caches.resolve.*
import org.jetbrains.kotlin.resolve.scopes.*
import org.jetbrains.kotlin.idea.util.*

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
            //TODO: keep existing imports? at least aliases (comments)

            val importInsertHelper = ImportInsertHelper.getInstance(file.getProject())
            val currentPackageName = file.getPackageFqName()

            val descriptorsToImport = detectDescriptorsToImport()

            val importsToGenerate = HashSet<ImportPath>()
            if (codeStyleSettings.PREFER_ALL_UNDER_IMPORTS) {
                val packageNames = HashMap<FqName, Int>() // package name to symbols to import count

                fun incUseCount(packageName: FqName): Int {
                    val count = packageNames.getOrPut(packageName, { 0 }) + 1
                    packageNames.put(packageName, count)
                    return count
                }

                fun decUseCount(packageName: FqName): Int {
                    val count = packageNames[packageName]!! - 1
                    if (count > 0) {
                        packageNames[packageName] = count
                    }
                    else {
                        packageNames.remove(packageName)
                    }
                    return count
                }

                val classNames = HashSet<FqName>()

                val builder = StringBuilder()
                builder.append("package ").append(IdeDescriptorRenderers.SOURCE_CODE.renderFqName(currentPackageName)).append("\n")

                for (descriptor in descriptorsToImport) {
                    val fqName = descriptor.importableFqNameSafe
                    val parentFqName = fqName.parent()
                    if (descriptor is PackageViewDescriptor || parentFqName.isRoot()) {
                        importsToGenerate.add(ImportPath(fqName, false))
                    }
                    else {
                        val useCount = incUseCount(parentFqName)

                        if (descriptor is ClassDescriptor) {
                            classNames.add(fqName)
                        }

                        if (parentFqName == currentPackageName) continue // in current package
                        if (importInsertHelper.isImportedWithDefault(ImportPath(fqName, false), file)) continue

                        if (useCount == 1) {
                            builder.append("import ").append(IdeDescriptorRenderers.SOURCE_CODE.renderFqName(parentFqName)).append(".*\n")
                            importsToGenerate.add(ImportPath(parentFqName, true))
                        }
                    }
                }

                // now check that all classes are really imported
                val fileWithImports = JetPsiFactory(file).createAnalyzableFile("Dummy.kt", builder.toString(), file)
                val scope = fileWithImports.getResolutionFacade().getFileTopLevelScope(fileWithImports)
                for (fqName in classNames) {
                    if (scope.getClassifier(fqName.shortName())?.importableFqNameSafe != fqName) {
                        // add explicit import if failed to import with * (or from current package)
                        importsToGenerate.add(ImportPath(fqName, false))

                        val packageName = fqName.parent()
                        if (decUseCount(packageName) == 0) { // all under import is not really needed
                            importsToGenerate.remove(ImportPath(packageName, true))
                        }
                    }
                }
            }
            else {
                for (descriptor in descriptorsToImport) {
                    val fqName = descriptor.importableFqNameSafe
                    if (descriptor !is PackageViewDescriptor && fqName.parent() == currentPackageName) continue // in current package
                    val importPath = ImportPath(fqName, false)
                    if (importInsertHelper.isImportedWithDefault(importPath, file)) continue
                    importsToGenerate.add(importPath)
                }
            }

            //TODO: drop unused aliases?
            aliasImports.mapTo(importsToGenerate) { ImportPath(it.getValue(), false, it.getKey())}

            val sortedImportsToGenerate = importsToGenerate.sortBy(importInsertHelper.importSortComparator)

            //TODO: do not touch file if everything is already correct?

            ApplicationManager.getApplication()!!.runWriteAction(Runnable {
                // remove old imports after adding new ones to keep imports folding state
                val oldImports = file.getImportDirectives()

                val importList = file.getImportList()!!
                val psiFactory = JetPsiFactory(file.getProject())
                for (importPath in sortedImportsToGenerate) {
                    importList.addBefore(psiFactory.createImportDirective(importPath), oldImports.lastOrNull()) // insert into the middle to keep collapsed state
                }

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

                override fun visitJetElement(element: JetElement) {
                    val reference = element.getReference()
                    if (reference is JetReference) {
                        val referencedName = (element as? JetNameReferenceExpression)?.getReferencedNameAsName() //TODO: other types of references

                        val targets = reference.resolveToDescriptors()
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

                            if (isAccessibleAsMember(target, element)) continue

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
            val scope = if (container.getKind() == ClassKind.CLASS_OBJECT)
                container.getContainingDeclaration() as? ClassDescriptor ?: return false
            else
                container
            val declaration = DescriptorToSourceUtils.classDescriptorToDeclaration(scope)
            return declaration != null && declaration.getContainingFile() == file && declaration.isAncestor(place)
        }
    }
}
