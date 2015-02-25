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

package org.jetbrains.kotlin.util

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
import org.jetbrains.kotlin.utils.addIfNotNull
import org.jetbrains.kotlin.resolve.scopes.JetScope
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue
import org.jetbrains.kotlin.descriptors.DeclarationDescriptorWithVisibility
import org.jetbrains.kotlin.resolve.descriptorUtil.getImportableDescriptor
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.idea.util.ImportInsertHelper
import org.jetbrains.kotlin.idea.util.ImportInsertHelper.ImportDescriptorResult
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.idea.refactoring.fqName.isImported
import java.util.*
import org.jetbrains.kotlin.idea.imports.*

public class ImportInsertHelperImpl(private val project: Project) : ImportInsertHelper() {

    override fun optimizeImportsOnTheFly(file: JetFile): Boolean {
        if (CodeInsightSettings.getInstance().OPTIMIZE_IMPORTS_ON_THE_FLY) {
            OptimizeImportsProcessor(project, file).runWithoutProgress()
            return true
        }
        else {
            return false
        }
    }

    private fun addImport(file: JetFile, importPath: ImportPath): JetImportDirective {
        val psiFactory = JetPsiFactory(project)
        if (file is JetCodeFragment) {
            val newDirective = psiFactory.createImportDirective(importPath)
            file.addImportsFromString(newDirective.getText())
            return newDirective
        }

        val importList = file.getImportList()
        if (importList != null) {
            val newDirective = psiFactory.createImportDirective(importPath)
            val imports = importList.getImports()
            if (imports.isEmpty()) { //TODO: strange hack
                importList.add(psiFactory.createNewLine())
                return importList.add(newDirective) as JetImportDirective
            }
            else {
                val insertAfter = imports
                        .reverse()
                        .firstOrNull {
                            val directivePath = it.getImportPath()
                            directivePath != null && ImportPathComparator.compare(directivePath, importPath) <= 0
                        }
                return importList.addAfter(newDirective, insertAfter) as JetImportDirective
            }
        }
        else {
            val newImportList = psiFactory.createImportDirectiveWithImportList(importPath)
            val packageDirective = file.getPackageDirective()
                                   ?: throw IllegalStateException("Scripts are not supported: " + file.getName())
            val addedImportList = packageDirective.getParent().addAfter(newImportList, packageDirective) as JetImportList
            return addedImportList.getImports().single()
        }
    }

    override val importSortComparator: Comparator<ImportPath>
        get() = ImportPathComparator

    private object ImportPathComparator : Comparator<ImportPath> {
        override fun compare(import1: ImportPath, import2: ImportPath): Int {
            // alias imports placed last
            if (import1.hasAlias() != import2.hasAlias()) {
                return if (import1.hasAlias()) +1 else -1
            }

            // standard library imports last
            val stdlib1 = isJavaOrKotlinStdlibImport(import1)
            val stdlib2 = isJavaOrKotlinStdlibImport(import2)
            if (stdlib1 != stdlib2) {
                return if (stdlib1) +1 else -1
            }

            return import1.toString().compareTo(import2.toString())
        }

        private fun isJavaOrKotlinStdlibImport(path: ImportPath): Boolean {
            val s = path.getPathStr()
            return s.startsWith("java.") || s.startsWith("javax.")|| s.startsWith("kotlin.")
        }
    }

    override fun isImportedWithDefault(importPath: ImportPath, contextFile: JetFile): Boolean {
        val defaultImports = if (ProjectStructureUtil.isJsKotlinModule(contextFile))
            TopDownAnalyzerFacadeForJS.DEFAULT_IMPORTS
        else
            TopDownAnalyzerFacadeForJVM.DEFAULT_IMPORTS
        return importPath.isImported(defaultImports)
    }

    override fun mayImportByCodeStyle(descriptor: DeclarationDescriptor): Boolean {
        val importable = descriptor.getImportableDescriptor()
        return when (importable) {
            is PackageViewDescriptor -> JetCodeStyleSettings.getInstance(project).IMPORT_PACKAGES
            else -> importable.getContainingDeclaration() is PackageFragmentDescriptor // do not import nested classes and non-top-level declarations
        }
    }

    override fun importDescriptor(file: JetFile, descriptor: DeclarationDescriptor): ImportDescriptorResult {
        return Importer(file).importDescriptor(descriptor)
    }

    private inner class Importer(
            private val file: JetFile
    ) {
        private val resolutionFacade = file.getResolutionFacade()
        private val nameCountToUseStarImport = JetCodeStyleSettings.getInstance(project).NAME_COUNT_TO_USE_STAR_IMPORT

        fun importDescriptor(descriptor: DeclarationDescriptor): ImportDescriptorResult {
            val target = descriptor.getImportableDescriptor()

            val name = target.getName()
            val topLevelScope = resolutionFacade.getFileTopLevelScope(file)

            val targetFqName = target.importableFqName ?: return ImportDescriptorResult.FAIL

            // check if import is not needed
            when (target) {
                is ClassDescriptor -> { if (topLevelScope.getClassifier(name)?.importableFqName == targetFqName) return ImportDescriptorResult.ALREADY_IMPORTED }
                is PackageViewDescriptor -> { if (topLevelScope.getPackage(name)?.importableFqName == targetFqName) return ImportDescriptorResult.ALREADY_IMPORTED }
                is FunctionDescriptor -> { if (topLevelScope.getFunctions(name).map { it.importableFqName }.contains(targetFqName)) return ImportDescriptorResult.ALREADY_IMPORTED }
                is PropertyDescriptor -> { if (topLevelScope.getProperties(name).map { it.importableFqName }.contains(targetFqName)) return ImportDescriptorResult.ALREADY_IMPORTED }
                else -> return ImportDescriptorResult.FAIL
            }

            if (!mayImportByCodeStyle(descriptor)) {
                return ImportDescriptorResult.FAIL
            }

            // cannot import for non-top level function or property
            if (target is CallableDescriptor && target.getContainingDeclaration() !is PackageFragmentDescriptor) {
                return ImportDescriptorResult.FAIL
            }

            val imports = file.getImportDirectives()

            //TODO: is that correct? What if function is imported and we need to import class?
            if (imports.any { it.getImportedName() == name.asString() }) return ImportDescriptorResult.FAIL

            val fqName = target.importableFqNameSafe
            val packageFqName = fqName.parent()

            val importsFromPackage = imports.count {
                val path = it.getImportPath()
                path != null && !path.isAllUnder() && !path.hasAlias() && path.fqnPart().parent() == packageFqName
            }

            val allUnderImportPath = ImportPath(packageFqName, true)
            val tryAllUnderImport = importsFromPackage + 1 >= nameCountToUseStarImport
                                    && !packageFqName.isRoot()
                                    && !imports.any { it.getImportPath() == allUnderImportPath }
                                    && when (target) {
                                        is ClassDescriptor -> topLevelScope.getClassifier(name) == null // this check does not give a guarantee that import with * will import the class - for example, there can be classes with conflicting name in more than one import with *
                                        is PackageViewDescriptor -> false
                                        is FunctionDescriptor, is PropertyDescriptor -> true
                                        else -> throw Exception()
                                    }

            if (tryAllUnderImport) {
                val result = addAllUnderImport(target)
                if (result != ImportDescriptorResult.FAIL) return result
            }

            return addExplicitImport(target)
        }

        private fun addAllUnderImport(target: DeclarationDescriptor): ImportDescriptorResult {
            val targetFqName = target.importableFqNameSafe
            val parentFqName = targetFqName.parent()

            val moduleDescriptor = resolutionFacade.findModuleDescriptor(file)
            val imports = file.getImportDirectives()
            val scopeToImport = getMemberScope(parentFqName, moduleDescriptor) ?: return ImportDescriptorResult.FAIL
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

            val filePackage = moduleDescriptor.getPackage(file.getPackageFqName())!!

            fun isVisible(descriptor: DeclarationDescriptor): Boolean {
                if (descriptor !is DeclarationDescriptorWithVisibility) return true
                val visibility = descriptor.getVisibility()
                return !visibility.mustCheckInImports() || Visibilities.isVisible(ReceiverValue.IRRELEVANT_RECEIVER, descriptor, filePackage)
            }

            val classNamesToImport = scopeToImport
                    .getDescriptorsFiltered(DescriptorKindFilter.CLASSIFIERS, { true })
                    .filter(::isVisible)
                    .map { it.getName() }

            val topLevelScope = resolutionFacade.getFileTopLevelScope(file)
            val conflictCandidates: List<ClassifierDescriptor> = classNamesToImport
                    .flatMap {
                        importedScopes.map { scope -> scope.getClassifier(it) }.filterNotNull()
                    }
                    .filter { importedClass ->
                        isVisible(importedClass)
                            && topLevelScope.getClassifier(importedClass.getName()) == importedClass /* check that class is really imported */
                            && imports.all { it.getImportPath() != ImportPath(importedClass.importableFqNameSafe, false)  } /* and not yet imported explicitly */
                    }
            val conflicts = detectNeededImports(conflictCandidates)

            val addedImport = addImport(parentFqName, true)

            if (target is ClassDescriptor) {
                val newTopLevelScope = resolutionFacade.getFileTopLevelScope(file)
                val resolvedTo = newTopLevelScope.getClassifier(target.getName())
                if (resolvedTo?.importableFqNameSafe != targetFqName) {
                    addedImport.delete()
                    return ImportDescriptorResult.FAIL
                }
            }

            for (conflict in conflicts) {
                addImport(DescriptorUtils.getFqNameSafe(conflict), false)
            }

            dropRedundantExplicitImports(parentFqName)

            return ImportDescriptorResult.IMPORT_ADDED
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

        private fun addExplicitImport(target: DeclarationDescriptor): ImportDescriptorResult {
            if (target is ClassDescriptor || target is PackageViewDescriptor) {
                val topLevelScope = resolutionFacade.getFileTopLevelScope(file)
                val name = target.getName()

                // check if there is a conflicting class imported with * import
                // (not with explicit import - explicit imports are checked before this method invocation)
                val classifier = topLevelScope.getClassifier(name)
                if (classifier != null && detectNeededImports(listOf(classifier)).isNotEmpty()) {
                    return ImportDescriptorResult.FAIL
                }
            }

            addImport(target.importableFqNameSafe, false)
            return ImportDescriptorResult.IMPORT_ADDED
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
            return this.getImportableTargets(resolutionFacade.analyze(this, BodyResolveMode.PARTIAL))
        }

        private fun addImport(fqName: FqName, allUnder: Boolean): JetImportDirective {
            return addImport(file, ImportPath(fqName, allUnder))
        }
    }
}
