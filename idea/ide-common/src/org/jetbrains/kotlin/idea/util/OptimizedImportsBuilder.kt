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

import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.idea.analysis.analyzeInContext
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.resolve.frontendService
import org.jetbrains.kotlin.idea.util.ImportInsertHelper
import org.jetbrains.kotlin.idea.util.getResolutionScope
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.renderer.render
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.ImportPath
import org.jetbrains.kotlin.resolve.lazy.FileScopeProvider
import org.jetbrains.kotlin.resolve.scopes.ImportingScope
import org.jetbrains.kotlin.resolve.scopes.utils.findClassifier
import org.jetbrains.kotlin.resolve.scopes.utils.parentsWithSelf
import org.jetbrains.kotlin.resolve.scopes.utils.replaceImportingScopes
import org.jetbrains.kotlin.utils.singletonOrEmptyList
import java.util.*

class OptimizedImportsBuilder(
        private val file: KtFile,
        private val data: InputData,
        private val options: Options
) {
    companion object {
        @TestOnly
        var testLog: StringBuilder? = null
    }

    interface AbstractReference {
        val element: KtElement
        val dependsOnNames: Collection<Name>
        fun resolve(bindingContext: BindingContext): Collection<DeclarationDescriptor>
    }

    data class InputData(
            val descriptorsToImport: Set<DeclarationDescriptor>,
            val references: Collection<AbstractReference>
    )

    data class Options(
            val nameCountToUseStarImport: Int,
            val nameCountToUseStarImportForMembers: Int,
            val isInPackagesToUseStarImport: (FqName) -> Boolean
    )

    private val importInsertHelper = ImportInsertHelper.getInstance(file.project)

    private sealed class LockedImport {
        // force presence of this import
        data class Positive(val importPath: ImportPath) : LockedImport() {
            override fun toString() = importPath.toString()
        }

        // force absence of this import
        data class Negative(val importPath: ImportPath) : LockedImport() {
            override fun toString() = "-" + importPath.toString()
        }
    }

    private val lockedImports = HashSet<LockedImport>()

    fun buildOptimizedImports(): List<ImportPath>? {
        // TODO: should we drop unused aliases?
        // keep all non-trivial aliases
        file.importDirectives
                .mapNotNull { it.importPath }
                .filter {
                    val aliasName = it.alias
                    aliasName != null && aliasName != it.fqnPart().shortName()
                }
                .mapTo(lockedImports) { LockedImport.Positive(it) }

        while (true) {
            val lockedImportsBefore = lockedImports.size
            val result = tryBuildOptimizedImports()
            if (lockedImports.size == lockedImportsBefore) return result
            testLog?.append("Trying to build import list again with locked imports: ${lockedImports.joinToString()}\n")
        }
    }

    private fun getExpressionToAnalyze(element: KtElement): KtExpression? {
        val parent = element.parent
        return when {
            parent is KtQualifiedExpression && element == parent.selectorExpression -> parent
            parent is KtCallExpression && element == parent.calleeExpression -> getExpressionToAnalyze(parent)
            parent is KtOperationExpression && element == parent.operationReference -> parent
            parent is KtUserType -> null //TODO: is it always correct?
            else -> element as? KtExpression //TODO: what if not expression? Example: KtPropertyDelegationMethodsReference
        }
    }

    private fun tryBuildOptimizedImports(): List<ImportPath>? {
        val importsToGenerate = HashSet<ImportPath>()
        lockedImports
                .filterIsInstance<LockedImport.Positive>()
                .mapTo(importsToGenerate) { it.importPath }

        val descriptorsByParentFqName = HashMap<FqName, MutableSet<DeclarationDescriptor>>()
        for (descriptor in data.descriptorsToImport) {
            val fqName = descriptor.importableFqName!!

            val explicitImportPath = ImportPath(fqName, false)
            if (explicitImportPath in importsToGenerate) continue

            val parentFqName = fqName.parent()
            val starImportPath = ImportPath(parentFqName, true)
            if (canUseStarImport(descriptor, fqName) && !starImportPath.isNegativeLocked()) {
                descriptorsByParentFqName.getOrPut(parentFqName) { HashSet() }.add(descriptor)
            }
            else {
                importsToGenerate.add(explicitImportPath)
            }
        }

        val classNamesToCheck = HashSet<FqName>()

        for (parentFqName in descriptorsByParentFqName.keys) {
            val starImportPath = ImportPath(parentFqName, true)
            if (starImportPath in importsToGenerate) continue

            val descriptors = descriptorsByParentFqName[parentFqName]!!
            val fqNames = descriptors.map { it.importableFqName!! }.toSet()
            val nameCountToUseStar = descriptors.first().nameCountToUseStar()
            val useExplicitImports = fqNames.size < nameCountToUseStar && !options.isInPackagesToUseStarImport(parentFqName)
                                     || starImportPath.isNegativeLocked()
            if (useExplicitImports) {
                fqNames
                        .filter { !isImportedByDefault(it) }
                        .mapTo(importsToGenerate) { ImportPath(it, false) }
            }
            else {
                descriptors
                        .filterIsInstance<ClassDescriptor>()
                        .mapTo(classNamesToCheck) { it.importableFqName!! }

                if (!fqNames.all { isImportedByDefault(it) }) {
                    importsToGenerate.add(starImportPath)
                }
            }
        }

        // now check that there are no conflicts and all classes are really imported
        addExplicitImportsForClassesWhenRequired(classNamesToCheck, descriptorsByParentFqName, importsToGenerate, file)

        val sortedImportsToGenerate = importsToGenerate.sortedWith(importInsertHelper.importSortComparator)

        // check if no changes to imports required
        val oldImports = file.importDirectives
        if (oldImports.size == sortedImportsToGenerate.size && oldImports.map { it.importPath } == sortedImportsToGenerate) return null

        val originalFileScope = file.getFileResolutionScope()
        val newFileScope = buildScopeByImports(file, sortedImportsToGenerate)

        var references = data.references
        if (testLog != null) {
            // to make log the same for all runs
            references = references.sortedBy { it.toString() }
        }
        for ((names, refs) in references.groupBy { it.dependsOnNames }) {
            if (!areScopeSlicesEqual(originalFileScope, newFileScope, names)) {
                for (ref in refs) {
                    val element = ref.element
                    val bindingContext = element.analyze()
                    val expressionToAnalyze = getExpressionToAnalyze(element) ?: continue
                    val newScope = element.getResolutionScope(bindingContext, file.getResolutionFacade()).replaceImportingScopes(newFileScope)
                    val newBindingContext = expressionToAnalyze.analyzeInContext(newScope, expressionToAnalyze)

                    testLog?.append("Additional checking of reference $ref\n")

                    val oldTargets = ref.resolve(bindingContext)
                    val newTargets = ref.resolve(newBindingContext)
                    if (!areTargetsEqual(oldTargets, newTargets)) {
                        testLog?.append("Changed resolve of $ref\n")
                        (oldTargets + newTargets).forEach { lockImportForDescriptor(it) }
                    }
                }
            }
        }

        return sortedImportsToGenerate
    }

    private fun lockImportForDescriptor(descriptor: DeclarationDescriptor) {
        val fqName = descriptor.importableFqName ?: return
        val explicitImportPath = ImportPath(fqName, false)
        val starImportPath = ImportPath(fqName.parent(), true)
        val importPaths = file.importDirectives.map { it.importPath }
        if (explicitImportPath in importPaths) {
            lockedImports.add(LockedImport.Positive(explicitImportPath))
        }
        else if (starImportPath in importPaths) {
            lockedImports.add(LockedImport.Positive(starImportPath))
        }
        else { // there is no import for this descriptor in the original import list, so do not allow to import it by star-import
            lockedImports.add(LockedImport.Negative(starImportPath))
        }
    }

    private fun addExplicitImportsForClassesWhenRequired(
            classNamesToCheck: Collection<FqName>,
            descriptorsByParentFqName: Map<FqName, MutableSet<DeclarationDescriptor>>,
            importsToGenerate: MutableSet<ImportPath>,
            originalFile: KtFile
    ) {
        val scope = buildScopeByImports(originalFile, importsToGenerate.filter { it.isAllUnder })
        for (fqName in classNamesToCheck) {
            if (scope.findClassifier(fqName.shortName(), NoLookupLocation.FROM_IDE)?.importableFqName != fqName) {
                // add explicit import if failed to import with * (or from current package)
                importsToGenerate.add(ImportPath(fqName, false))

                val parentFqName = fqName.parent()

                val siblingsToImport = descriptorsByParentFqName[parentFqName]!!
                for (descriptor in siblingsToImport.filter { it.importableFqName == fqName }) {
                    siblingsToImport.remove(descriptor)
                }

                if (siblingsToImport.isEmpty()) { // star import is not really needed
                    importsToGenerate.remove(ImportPath(parentFqName, true))
                }
            }
        }
    }

    private fun buildScopeByImports(originalFile: KtFile, importsToGenerate: Collection<ImportPath>): ImportingScope {
        val fileText = buildString {
            append("package ")
            append(originalFile.packageFqName.toUnsafe().render())
            append("\n")

            for (importPath in importsToGenerate) {
                append("import ")
                append(importPath.pathStr)
                if (importPath.hasAlias()) {
                    append("=")
                    append(importPath.alias!!.render())
                }
                append("\n")
            }
        }
        val fileWithImports = KtPsiFactory(originalFile).createAnalyzableFile("Dummy.kt", fileText, originalFile)
        return fileWithImports.getFileResolutionScope()
    }

    private fun KtFile.getFileResolutionScope() = getResolutionFacade().frontendService<FileScopeProvider>().getFileScopes(this).importingScope

    private fun areScopeSlicesEqual(scope1: ImportingScope, scope2: ImportingScope, names: Collection<Name>): Boolean {
        val tower1 = scope1.extractSliceTower(names)
        val tower2 = scope2.extractSliceTower(names)
        val iterator1 = tower1.iterator()
        val iterator2 = tower2.iterator()
        while (true) {
            if (!iterator1.hasNext()) {
                return !iterator2.hasNext()
            }
            else if (!iterator2.hasNext()) {
                return false
            }
            else {
                if (!areTargetsEqual(iterator1.next(), iterator2.next())) return false
            }
        }
    }

    private fun ImportingScope.extractSliceTower(names: Collection<Name>): Sequence<Collection<DeclarationDescriptor>> {
        return parentsWithSelf
                .map { scope ->
                    names.flatMap { name ->
                        scope.getContributedFunctions(name, NoLookupLocation.FROM_IDE) +
                        scope.getContributedVariables(name, NoLookupLocation.FROM_IDE) +
                        scope.getContributedClassifier(name, NoLookupLocation.FROM_IDE).singletonOrEmptyList()
                    }
                }
                .filter { it.isNotEmpty() }
    }

    private fun canUseStarImport(descriptor: DeclarationDescriptor, fqName: FqName): Boolean {
        return when {
            fqName.parent().isRoot -> false
            (descriptor.containingDeclaration as? ClassDescriptor)?.kind == ClassKind.OBJECT -> false
            else -> true
        }
    }

    private fun isImportedByDefault(fqName: FqName) = importInsertHelper.isImportedWithDefault(ImportPath(fqName, false), file)

    private fun DeclarationDescriptor.nameCountToUseStar(): Int {
        val isMember = containingDeclaration is ClassDescriptor
        return if (isMember)
            options.nameCountToUseStarImportForMembers
        else
            options.nameCountToUseStarImport
    }

    private fun areTargetsEqual(descriptors1: Collection<DeclarationDescriptor>, descriptors2: Collection<DeclarationDescriptor>): Boolean {
        return descriptors1.size == descriptors2.size &&
               descriptors1.zip(descriptors2).all { it.first.importableFqName == it.second.importableFqName } //TODO: can have different order?
    }

    private fun ImportPath.isNegativeLocked(): Boolean = lockedImports.any { it is LockedImport.Negative && it.importPath == this }
}