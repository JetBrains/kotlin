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

@file:JvmName("ImportsUtils")

package org.jetbrains.kotlin.idea.imports

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.util.ImportInsertHelper
import org.jetbrains.kotlin.idea.util.getFileResolutionScope
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.KtReferenceExpression
import org.jetbrains.kotlin.renderer.render
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.ImportPath
import org.jetbrains.kotlin.resolve.bindingContextUtil.getReferenceTargets
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.descriptorUtil.getImportableDescriptor
import org.jetbrains.kotlin.resolve.scopes.utils.findClassifier
import org.jetbrains.kotlin.types.KotlinType
import java.util.*

object ImportPathComparator : Comparator<ImportPath> {
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
        val s = path.pathStr
        return s.startsWith("java.") || s.startsWith("javax.")|| s.startsWith("kotlin.")
    }
}

val DeclarationDescriptor.importableFqName: FqName?
    get() {
        if (!canBeReferencedViaImport()) return null
        return getImportableDescriptor().fqNameSafe
    }

fun DeclarationDescriptor.canBeReferencedViaImport(): Boolean {
    if (this is PackageViewDescriptor ||
        DescriptorUtils.isTopLevelDeclaration(this) ||
        this is CallableDescriptor && DescriptorUtils.isStaticDeclaration(this)) {
        return !name.isSpecial
    }

    val parentClass = containingDeclaration as? ClassDescriptor ?: return false
    if (!parentClass.canBeReferencedViaImport()) return false

    return when (this) {
        is ConstructorDescriptor -> !parentClass.isInner // inner class constructors can't be referenced via import
        is ClassDescriptor -> true
        else -> parentClass.kind == ClassKind.OBJECT
    }
}

fun KotlinType.canBeReferencedViaImport(): Boolean {
    val descriptor = constructor.declarationDescriptor
    return descriptor != null && descriptor.canBeReferencedViaImport()
}

// for cases when class qualifier refers companion object treats it like reference to class itself
fun KtReferenceExpression.getImportableTargets(bindingContext: BindingContext): Collection<DeclarationDescriptor> {
    val targets = bindingContext[BindingContext.SHORT_REFERENCE_TO_COMPANION_OBJECT, this]?.let { listOf(it) }
                  ?: getReferenceTargets(bindingContext)
    return targets.map { it.getImportableDescriptor() }.toSet()
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
