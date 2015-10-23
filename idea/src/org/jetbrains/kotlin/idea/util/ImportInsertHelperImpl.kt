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

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.PsiModificationTrackerImpl
import com.intellij.psi.util.PsiModificationTracker
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.idea.caches.resolve.getFileKtScope
import org.jetbrains.kotlin.idea.caches.resolve.getFileScopeChain
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.core.formatter.JetCodeStyleSettings
import org.jetbrains.kotlin.idea.core.targetDescriptors
import org.jetbrains.kotlin.idea.imports.getImportableTargets
import org.jetbrains.kotlin.idea.imports.importableFqName
import org.jetbrains.kotlin.idea.project.platform
import org.jetbrains.kotlin.idea.refactoring.fqName.isImported
import org.jetbrains.kotlin.idea.util.ImportInsertHelper
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.ImportPath
import org.jetbrains.kotlin.resolve.descriptorUtil.getImportableDescriptor
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.KtScope
import org.jetbrains.kotlin.resolve.scopes.getDescriptorsFiltered
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue
import org.jetbrains.kotlin.resolve.scopes.utils.getClassifier
import org.jetbrains.kotlin.utils.addIfNotNull
import java.util.*

public class ImportInsertHelperImpl(private val project: Project) : ImportInsertHelper() {

    private val codeStyleSettings: JetCodeStyleSettings
        get() = JetCodeStyleSettings.getInstance(project)

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

    override fun isImportedWithDefault(importPath: ImportPath, contextFile: KtFile): Boolean {
        val defaultImports = contextFile.platform.defaultModuleParameters.defaultImports
        return importPath.isImported(defaultImports)
    }

    override fun mayImportOnShortenReferences(descriptor: DeclarationDescriptor): Boolean {
        val importable = descriptor.getImportableDescriptor()
        return when (importable) {
            is PackageViewDescriptor -> false // now package cannot be imported

            is ClassDescriptor -> {
                importable.getContainingDeclaration() is PackageFragmentDescriptor
                    || codeStyleSettings.IMPORT_NESTED_CLASSES
            }

            else -> importable.getContainingDeclaration() is PackageFragmentDescriptor // do not import members (e.g. java static members)
        }
    }

    override fun importDescriptor(file: KtFile, descriptor: DeclarationDescriptor)
            = Importer(file).importDescriptor(descriptor)

    private inner class Importer(
            private val file: KtFile
    ) {
        private val resolutionFacade = file.getResolutionFacade()

        private fun isAlreadyImported(target: DeclarationDescriptor, topLevelScope: KtScope, targetFqName: FqName): Boolean {
            val name = target.name
            when (target) {
                is ClassDescriptor -> {
                    val classifier = topLevelScope.getClassifier(name, NoLookupLocation.FROM_IDE)
                    if (classifier?.importableFqName == targetFqName) return true
                }
                is FunctionDescriptor -> {
                    val functions = topLevelScope.getFunctions(name, NoLookupLocation.FROM_IDE)
                    if (functions.map { it.importableFqName }.contains(targetFqName)) return true
                }
                is PropertyDescriptor -> {
                    val properties = topLevelScope.getProperties(name, NoLookupLocation.FROM_IDE)
                    if (properties.map { it.importableFqName }.contains(targetFqName)) return true
                }
            }
            return false
        }

        fun importDescriptor(descriptor: DeclarationDescriptor): ImportDescriptorResult {
            val target = descriptor.getImportableDescriptor()

            val name = target.name
            val topLevelScope = resolutionFacade.getFileKtScope(file)

            // check if import is not needed
            val targetFqName = target.importableFqName ?: return ImportDescriptorResult.FAIL
            if (isAlreadyImported(target, topLevelScope, targetFqName)) return ImportDescriptorResult.ALREADY_IMPORTED

            val imports = if (file is KtCodeFragment)
                file.importsAsImportList()?.getImports() ?: listOf()
            else
                file.getImportDirectives()

            if (imports.any { !it.isAllUnder && it.importPath?.fqnPart() == targetFqName }) {
                return ImportDescriptorResult.FAIL
            }

            // check there is an explicit import of a class/package with the same name already
            val conflict = when (target) {
                is ClassDescriptor -> topLevelScope.getClassifier(name, NoLookupLocation.FROM_IDE)
                is PackageViewDescriptor -> topLevelScope.getPackage(name)
                else -> null
            }
            if (conflict != null && imports.any {
                !it.isAllUnder()
                && it.getImportPath()?.fqnPart() == conflict.importableFqName
                && it.getImportPath()?.getImportedName() == name
            }) {
                return ImportDescriptorResult.FAIL
            }

            val fqName = target.importableFqName!!
            val packageFqName = fqName.parent()

            val tryStarImport = shouldTryStarImport(packageFqName, target, imports)
                                    && when (target) {
                                        // this check does not give a guarantee that import with * will import the class - for example,
                                        // there can be classes with conflicting name in more than one import with *
                                        is ClassDescriptor -> topLevelScope.getClassifier(name, NoLookupLocation.FROM_IDE) == null
                                        is FunctionDescriptor, is PropertyDescriptor -> true
                                        else -> error("Unknown kind of descriptor to import:$target")
                                    }

            if (tryStarImport) {
                val result = addStarImport(target)
                if (result != ImportDescriptorResult.FAIL) return result
            }

            return addExplicitImport(target)
        }

        private fun shouldTryStarImport(containerFqName: FqName, target: DeclarationDescriptor, imports: Collection<KtImportDirective>): Boolean {
            if (containerFqName.isRoot) return false

            val container = target.containingDeclaration
            if (container is ClassDescriptor && container.kind == ClassKind.OBJECT) return false // cannot import with '*' from object

            val starImportPath = ImportPath(containerFqName, true)
            if (imports.any { it.getImportPath() == starImportPath }) return false

            if (containerFqName.asString() in codeStyleSettings.PACKAGES_TO_USE_STAR_IMPORTS) return true

            val importsFromPackage = imports.count {
                val path = it.getImportPath()
                path != null && !path.isAllUnder() && !path.hasAlias() && path.fqnPart().parent() == containerFqName
            }
            val nameCountToUseStar = if (container is ClassDescriptor)
                codeStyleSettings.NAME_COUNT_TO_USE_STAR_IMPORT_FOR_MEMBERS
            else
                codeStyleSettings.NAME_COUNT_TO_USE_STAR_IMPORT
            return importsFromPackage + 1 >= nameCountToUseStar
        }

        private fun addStarImport(target: DeclarationDescriptor): ImportDescriptorResult {
            val targetFqName = target.importableFqName!!
            val parentFqName = targetFqName.parent()

            val moduleDescriptor = resolutionFacade.moduleDescriptor
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

            val filePackage = moduleDescriptor.getPackage(file.getPackageFqName())

            fun isVisible(descriptor: DeclarationDescriptor): Boolean {
                if (descriptor !is DeclarationDescriptorWithVisibility) return true
                val visibility = descriptor.getVisibility()
                return !visibility.mustCheckInImports() || Visibilities.isVisible(ReceiverValue.IRRELEVANT_RECEIVER, descriptor, filePackage)
            }

            val classNamesToImport = scopeToImport
                    .getDescriptorsFiltered(DescriptorKindFilter.CLASSIFIERS, { true })
                    .filter(::isVisible)
                    .map { it.getName() }

            val topLevelScope = resolutionFacade.getFileScopeChain(file)
            val conflictCandidates: List<ClassifierDescriptor> = classNamesToImport
                    .flatMap {
                        importedScopes.map { scope -> scope.getClassifier(it, NoLookupLocation.FROM_IDE) }.filterNotNull()
                    }
                    .filter { importedClass ->
                        isVisible(importedClass)
                            // check that class is really imported
                            && topLevelScope.getClassifier(importedClass.name, NoLookupLocation.FROM_IDE) == importedClass
                            // and not yet imported explicitly
                            && imports.all { it.importPath != ImportPath(importedClass.importableFqName!!, false)  }
                    }
            val conflicts = detectNeededImports(conflictCandidates)

            val addedImport = addImport(parentFqName, true)

            val newTopLevelScope = resolutionFacade.getFileKtScope(file)
            if (!isAlreadyImported(target, newTopLevelScope, targetFqName)) {
                addedImport.delete()
                return ImportDescriptorResult.FAIL
            }

            for (conflict in conflicts) {
                addImport(DescriptorUtils.getFqNameSafe(conflict), false)
            }

            dropRedundantExplicitImports(parentFqName)

            return ImportDescriptorResult.IMPORT_ADDED
        }

        private fun getMemberScope(fqName: FqName, moduleDescriptor: ModuleDescriptor): KtScope? {
            val packageView = moduleDescriptor.getPackage(fqName)
            if (!packageView.isEmpty()) {
                return packageView.memberScope
            }

            val parentScope = getMemberScope(fqName.parent(), moduleDescriptor) ?: return null
            val classifier = parentScope.getClassifier(fqName.shortName(), NoLookupLocation.FROM_IDE)
            val classDescriptor = classifier as? ClassDescriptor ?: return null
            return classDescriptor.getDefaultType().getMemberScope()
        }

        private fun addExplicitImport(target: DeclarationDescriptor): ImportDescriptorResult {
            if (target is ClassDescriptor || target is PackageViewDescriptor) {
                val topLevelScope = resolutionFacade.getFileScopeChain(file)
                val name = target.getName()

                // check if there is a conflicting class imported with * import
                // (not with explicit import - explicit imports are checked before this method invocation)
                val classifier = topLevelScope.getClassifier(name, NoLookupLocation.FROM_IDE)
                if (classifier != null && detectNeededImports(listOf(classifier)).isNotEmpty()) {
                    return ImportDescriptorResult.FAIL
                }
            }

            addImport(target.importableFqName!!, false)
            return ImportDescriptorResult.IMPORT_ADDED
        }

        private fun dropRedundantExplicitImports(packageFqName: FqName) {
            val dropCandidates = file.getImportDirectives().filter {
                !it.isAllUnder() && it.getAliasName() == null && it.getImportPath()?.fqnPart()?.parent() == packageFqName
            }

            val importsToCheck = ArrayList<FqName>()
            for (import in dropCandidates) {
                if (import.importedReference == null) continue
                val targets = import.targetDescriptors()
                if (targets.any { it is PackageViewDescriptor }) continue // do not drop import of package
                val classDescriptor = targets.filterIsInstance<ClassDescriptor>().firstOrNull()
                importsToCheck.addIfNotNull(classDescriptor?.importableFqName)
                import.delete()
            }

            if (importsToCheck.isNotEmpty()) {
                val topLevelScope = resolutionFacade.getFileScopeChain(file)
                for (classFqName in importsToCheck) {
                    val classifier = topLevelScope.getClassifier(classFqName.shortName(), NoLookupLocation.FROM_IDE)
                    if (classifier?.importableFqName != classFqName) {
                        addImport(classFqName, false) // restore explicit import
                    }
                }
            }
        }

        private fun detectNeededImports(importedClasses: Collection<ClassifierDescriptor>): Set<ClassifierDescriptor> {
            if (importedClasses.isEmpty()) return setOf()

            val classesToCheck = importedClasses.map { it.getName() to it }.toMap().toLinkedMap()
            val result = LinkedHashSet<ClassifierDescriptor>()
            file.accept(object : KtVisitorVoid() {
                override fun visitElement(element: PsiElement) {
                    if (classesToCheck.isEmpty()) return
                    element.acceptChildren(this)
                }

                override fun visitImportList(importList: KtImportList) {
                }

                override fun visitPackageDirective(directive: KtPackageDirective) {
                }

                override fun visitSimpleNameExpression(expression: KtSimpleNameExpression) {
                    if (KtPsiUtil.isSelectorInQualified(expression)) return

                    val refName = expression.getReferencedNameAsName()
                    val descriptor = classesToCheck[refName]
                    if (descriptor != null) {
                        val targetFqName = targetFqName(expression)
                        if (targetFqName != null && targetFqName == DescriptorUtils.getFqNameSafe(descriptor)) {
                            classesToCheck.remove(refName)
                            result.add(descriptor)
                        }
                    }
                }
            })
            return result
        }

        private fun targetFqName(ref: KtReferenceExpression): FqName?
                = ref.resolveTargets().map { it.importableFqName }.toSet().singleOrNull()

        private fun KtReferenceExpression.resolveTargets(): Collection<DeclarationDescriptor>
                = this.getImportableTargets(resolutionFacade.analyze(this, BodyResolveMode.PARTIAL))

        private fun addImport(fqName: FqName, allUnder: Boolean): KtImportDirective {
            //TODO: it's a temporary hack for JetCodeFragment's and non-physical files
            // We should increment modification tracker after inserting import to invalidate resolve caches.
            // Without this modification references with new import won't be resolved.
            (PsiModificationTracker.SERVICE.getInstance(project) as PsiModificationTrackerImpl).incOutOfCodeBlockModificationCounter()

            val importPath = ImportPath(fqName, allUnder)

            val psiFactory = KtPsiFactory(project)
            if (file is KtCodeFragment) {
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
                    return importList.add(newDirective) as KtImportDirective
                }
                else {
                    val insertAfter = imports
                            .lastOrNull {
                                val directivePath = it.getImportPath()
                                directivePath != null && ImportPathComparator.compare(directivePath, importPath) <= 0
                            }
                    return importList.addAfter(newDirective, insertAfter) as KtImportDirective
                }
            }
            else {
                error("Trying to insert import $fqName into a file ${file.getName()} of type ${file.javaClass} with no import list.")
            }
        }
    }
}
