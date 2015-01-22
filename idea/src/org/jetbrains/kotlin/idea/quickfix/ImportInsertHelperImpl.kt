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

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.codeInsight.CodeInsightSettings
import com.intellij.codeInsight.actions.OptimizeImportsProcessor
import org.jetbrains.kotlin.idea.project.ProjectStructureUtil
import org.jetbrains.kotlin.js.analyze.TopDownAnalyzerFacadeForJS
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.ImportPath
import org.jetbrains.kotlin.resolve.jvm.TopDownAnalyzerFacadeForJVM
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.descriptors.PackageViewDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.formatter.JetCodeStyleSettings
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.scopes.getDescriptorsFiltered
import org.jetbrains.kotlin.descriptors.ClassifierDescriptor
import org.jetbrains.kotlin.idea.imports.importableFqName
import java.util.LinkedHashSet
import org.jetbrains.kotlin.idea.imports.importableFqNameSafe
import java.util.ArrayList
import org.jetbrains.kotlin.utils.addIfNotNull
import org.jetbrains.kotlin.resolve.scopes.JetScope
import org.jetbrains.kotlin.descriptors.ModuleDescriptor

public class ImportInsertHelperImpl : ImportInsertHelper {
    /**
     * Add import directive into the PSI tree for the given package.
     *
     * @param importFqn full name of the import
     * @param file File where directive should be added.
     */
    override fun addImportDirectiveIfNeeded(importFqn: FqName, file: JetFile) {
        val importPath = ImportPath(importFqn, false)

        optimizeImportsOnTheFly(file)

        if (needImport(importPath, file)) {
            writeImportToFile(importPath, file)
        }
    }

    override fun optimizeImportsOnTheFly(file: JetFile): Boolean {
        if (CodeInsightSettings.getInstance().OPTIMIZE_IMPORTS_ON_THE_FLY) {
            OptimizeImportsProcessor(file.getProject(), file).runWithoutProgress()
            return true
        }
        else {
            return false
        }
    }

    override fun writeImportToFile(importPath: ImportPath, file: JetFile): JetImportDirective {
        val psiFactory = JetPsiFactory(file.getProject())
        if (file is JetCodeFragment) {
            val newDirective = psiFactory.createImportDirective(importPath)
            file.addImportsFromString(newDirective.getText())
            return newDirective
        }

        val importList = file.getImportList()
        if (importList != null) {
            val newDirective = psiFactory.createImportDirective(importPath)
            importList.add(psiFactory.createNewLine())
            return importList.add(newDirective) as JetImportDirective
        }
        else {
            val newImportList = psiFactory.createImportDirectiveWithImportList(importPath)
            val packageDirective = file.getPackageDirective()
            if (packageDirective == null) {
                throw IllegalStateException("Scripts are not supported: " + file.getName())
            }

            val addedImportList = packageDirective.getParent().addAfter(newImportList, packageDirective) as JetImportList
            return addedImportList.getImports().single()
        }
    }

    /**
     * Check that import is useless.
     */
    private fun isImportedByDefault(importPath: ImportPath, jetFile: JetFile): Boolean {
        if (importPath.fqnPart().isRoot()) {
            return true
        }

        if (!importPath.isAllUnder() && !importPath.hasAlias()) {
            // Single element import without .* and alias is useless
            if (importPath.fqnPart().isOneSegmentFQN()) {
                return true
            }

            // There's no need to import a declaration from the package of current file
            if (jetFile.getPackageFqName() == importPath.fqnPart().parent()) {
                return true
            }
        }

        return isImportedWithDefault(importPath, jetFile)
    }

    override fun isImportedWithDefault(importPath: ImportPath, contextFile: JetFile): Boolean {
        val defaultImports = if (ProjectStructureUtil.isJsKotlinModule(contextFile))
            TopDownAnalyzerFacadeForJS.DEFAULT_IMPORTS
        else
            TopDownAnalyzerFacadeForJVM.DEFAULT_IMPORTS
        return importPath.isImported(defaultImports)
    }

    override fun needImport(importPath: ImportPath, file: JetFile, importDirectives: List<JetImportDirective>): Boolean {
        if (isImportedByDefault(importPath, file)) {
            return false
        }

        if (!importDirectives.isEmpty()) {
            // Check if import is already present
            for (directive in importDirectives) {
                val existentImportPath = directive.getImportPath()
                if (existentImportPath != null && importPath.isImported(existentImportPath)) {
                    return false
                }
            }
        }

        return true
    }

    override fun importDescriptor(file: JetFile, descriptor: DeclarationDescriptor): Boolean {
        return Importer(file).importDescriptor(descriptor)
    }

    private inner class Importer(
            private val file: JetFile
    ) {
        private val resolutionFacade = file.getResolutionFacade()
        private val preferAllUnderImports = JetCodeStyleSettings.getInstance(file.getProject()).PREFER_ALL_UNDER_IMPORTS

        fun importDescriptor(descriptor: DeclarationDescriptor): Boolean {
            val target = if (DescriptorUtils.isClassObject(descriptor)) // references to class object are treated as ones to its owner class
                descriptor.getContainingDeclaration() as? ClassDescriptor ?: return false
            else
                descriptor

            val name = target.getName()
            val topLevelScope = resolutionFacade.getFileTopLevelScope(file)

            // check if import is not needed
            when (target) {
                is ClassDescriptor -> { if (topLevelScope.getClassifier(name) == target) return true }
                is PackageViewDescriptor -> { if (topLevelScope.getPackage(name) == target) return true }
                is FunctionDescriptor -> { if (topLevelScope.getFunctions(name).contains(target)) return true }
                is PropertyDescriptor -> { if (topLevelScope.getProperties(name).contains(target)) return true }
                else -> return false
            }

            // do not insert imports for non-top level declarations
            if (target !is PackageViewDescriptor && target.getContainingDeclaration() !is PackageFragmentDescriptor) return false

            val imports = file.getImportDirectives()

            //TODO: is that correct? What if function is imported and we need to import class?
            if (imports.any { it.getImportedName() == name.asString() }) return false

            val fqName = target.importableFqNameSafe
            val packageFqName = fqName.parent()

            val allUnderImportPath = ImportPath(packageFqName, true)
            val tryAllUnderImport = preferAllUnderImports
                                    && !packageFqName.isRoot()
                                    && !imports.any { it.getImportPath() == allUnderImportPath }
                                    && when (target) {
                                        is ClassDescriptor -> topLevelScope.getClassifier(name) == null // this check does not give a guarantee that import with * will import the class - for example, there can be classes with conflicting name in more than one import with *
                                        is PackageViewDescriptor -> false
                                        is FunctionDescriptor, is PropertyDescriptor -> true
                                        else -> throw Exception()
                                    }

            if (tryAllUnderImport && addAllUnderImport(target)) {
                return true
            }

            return addExplicitImport(target)
        }

        private fun addAllUnderImport(target: DeclarationDescriptor): Boolean {
            val targetFqName = target.importableFqNameSafe
            val parentFqName = targetFqName.parent()

            val moduleDescriptor = resolutionFacade.findModuleDescriptor(file)
            val imports = file.getImportDirectives()
            val scopeToImport = getMemberScope(parentFqName, moduleDescriptor) ?: return false
            val importedScopes = imports
                    .filter { it.isAllUnder () }
                    .map {
                        val importPath = it.getImportPath()
                        if (importPath != null) {
                            val fqName = importPath.fqnPart()
                            getMemberScope(fqName, moduleDescriptor)
                        }
                        else {
                            null
                        }
                    }
                    .filterNotNull()

            val classNamesToImport = scopeToImport
                    .getDescriptorsFiltered(DescriptorKindFilter.CLASSIFIERS, { true })
                    .map { it.getName() }

            val aliasNames = imports.map { it.getImportedName() }.filterNotNull().toSet()
            //TODO: check for visibility
            var conflictCandidates: List<ClassifierDescriptor> = classNamesToImport
                    .filter { it.asString() !in aliasNames }
                    .flatMap {
                        importedScopes.map { scope -> scope.getClassifier(it) }.filterNotNull()
                    }
            val conflicts = detectNeededImports(conflictCandidates)

            val addedImport = addImport(parentFqName, true)

            if (target is ClassDescriptor) {
                val newTopLevelScope = resolutionFacade.getFileTopLevelScope(file)
                val resolvedTo = newTopLevelScope.getClassifier(target.getName())
                if (resolvedTo?.importableFqNameSafe != targetFqName) {
                    addedImport.delete()
                    return false
                }
            }

            for (conflict in conflicts) {
                addImport(DescriptorUtils.getFqNameSafe(conflict), false)
            }

            dropRedundantExplicitImports(parentFqName)

            return true
        }

        private fun getMemberScope(fqName: FqName, moduleDescriptor: ModuleDescriptor): JetScope? {
            val packageView = moduleDescriptor.getPackage(fqName)
            if (packageView != null) {
                return packageView.getMemberScope()
            }

            val parentScope = getMemberScope(fqName.parent(), moduleDescriptor) ?: return null
            val classDescriptor = parentScope.getClassifier(fqName.shortName()) as? ClassDescriptor ?: return null
            return classDescriptor.getDefaultType().getMemberScope()
        }

        private fun addExplicitImport(target: DeclarationDescriptor): Boolean {
            if (target is ClassDescriptor || target is PackageViewDescriptor) {
                val topLevelScope = resolutionFacade.getFileTopLevelScope(file)
                val name = target.getName()

                val classifier = topLevelScope.getClassifier(name)
                if (classifier != null && detectNeededImports(listOf(classifier)).isNotEmpty()) {
                    return false
                }
            }

            addImport(target.importableFqNameSafe, false)
            return true
        }

        private fun dropRedundantExplicitImports(packageFqName: FqName) {
            val dropCandidates = file.getImportDirectives().filter {
                !it.isAllUnder() && it.getAliasName() == null && it.getImportPath()?.fqnPart()?.parent() == packageFqName
            }

            val importsToCheck = ArrayList<FqName>()
            for (import in dropCandidates) {
                val importedReference = import.getImportedReference() ?: continue
                val refExpr = (importedReference as JetDotQualifiedExpression).getSelectorExpression() as JetSimpleNameExpression
                val targets = refExpr.resolveTargets()
                if (targets.any { it is PackageViewDescriptor }) continue // do not drop import of package
                val classDescriptor = targets.filterIsInstance<ClassDescriptor>().firstOrNull()
                importsToCheck.addIfNotNull(classDescriptor?.importableFqNameSafe)
                import.delete()
            }

            if (importsToCheck.isNotEmpty()) {
                val topLevelScope = resolutionFacade.getFileTopLevelScope(file)
                for (classFqName in importsToCheck) {
                    if (topLevelScope.getClassifier(classFqName.shortName())?.importableFqNameSafe != classFqName) {
                        addImport(classFqName, false) // restore explicit import
                    }
                }
            }
        }

        private fun detectNeededImports(importedDescriptors: Collection<DeclarationDescriptor>): Set<DeclarationDescriptor> {
            if (importedDescriptors.isEmpty()) return setOf()

            val descriptorsToCheck = importedDescriptors.map { it.getName() to it }.toMap().toLinkedMap()
            val result = LinkedHashSet<DeclarationDescriptor>()
            file.accept(object : JetVisitorVoid() {
                override fun visitElement(element: PsiElement) {
                    if (descriptorsToCheck.isEmpty()) return
                    element.acceptChildren(this)
                }

                override fun visitImportList(importList: JetImportList) {
                }

                override fun visitPackageDirective(directive: JetPackageDirective) {
                }

                override fun visitSimpleNameExpression(expression: JetSimpleNameExpression) {
                    if (JetPsiUtil.isSelectorInQualified(expression)) return

                    val refName = expression.getReferencedNameAsName()
                    val descriptor = descriptorsToCheck[refName]
                    if (descriptor != null) {
                        val targetFqName = targetFqName(expression)
                        if (targetFqName != null && targetFqName == DescriptorUtils.getFqNameSafe(descriptor)) {
                            descriptorsToCheck.remove(refName)
                            result.add(descriptor)
                        }
                    }
                }
            })
            return result
        }

        private fun JetImportDirective.getImportedName(): String? = JetPsiUtil.getAliasName(this)?.getIdentifier()

        private fun targetFqName(ref: JetReferenceExpression): FqName? {
            return ref.resolveTargets().map { it.importableFqName }.toSet().singleOrNull()

        }

        private fun JetReferenceExpression.resolveTargets(): Collection<DeclarationDescriptor> {
            val bindingContext = resolutionFacade.analyze(this, BodyResolveMode.PARTIAL)
            return bindingContext[BindingContext.REFERENCE_TARGET, this]?.let { listOf(it) }
                          ?: bindingContext[BindingContext.AMBIGUOUS_REFERENCE_TARGET, this]
                          ?: return listOf()
        }

        private fun addImport(fqName: FqName, allUnder: Boolean): JetImportDirective {
            return writeImportToFile(ImportPath(fqName, allUnder), file)
        }
    }
}
