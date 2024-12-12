// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.kotlin.analysis.api.dumdum

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.api.dumdum.index.StubIndex
import org.jetbrains.kotlin.analysis.api.dumdum.stubindex.KotlinAnnotationsIndex
import org.jetbrains.kotlin.analysis.api.dumdum.stubindex.KotlinFullClassNameIndex
import org.jetbrains.kotlin.analysis.api.platform.declarations.KotlinAnnotationsResolver
import org.jetbrains.kotlin.analysis.api.platform.declarations.KotlinAnnotationsResolverFactory
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*

internal class IdeKotlinAnnotationsResolverFactory(
    private val project: Project,
    private val stubIndex: StubIndex
) : KotlinAnnotationsResolverFactory {
    override fun createAnnotationResolver(searchScope: GlobalSearchScope): KotlinAnnotationsResolver {
        return IdeKotlinAnnotationsResolver(project, searchScope, stubIndex)
    }
}

/**
 * IDE-mode implementation for [KotlinAnnotationsResolver].
 *
 * Uses indices and PSI as a way to "resolve" the annotations, so it might not be 100% accurate.
 *
 * @param searchScope A scope in which [IdeKotlinAnnotationsResolver] will operate.
 */
private class IdeKotlinAnnotationsResolver(
    private val project: Project,
    private val searchScope: GlobalSearchScope,
    private val stubIndex: StubIndex,
) : KotlinAnnotationsResolver {
    override fun declarationsByAnnotation(annotationClassId: ClassId): Set<KtAnnotated> {
        require(!annotationClassId.isLocal && !annotationClassId.isNestedClass) {
            "Queried annotation must be top-level, but was $annotationClassId"
        }

        val annotationEntries = KotlinAnnotationsIndex.get(stubIndex, annotationClassId.shortClassName.asString(), project, searchScope)

        return annotationEntries.asSequence()
            .filter { it.resolveAnnotationId() == annotationClassId }
            .mapNotNull { it.annotationOwner }
            .filter { it is KtFile || it is KtDeclaration }
            .toSet()
    }

    override fun annotationsOnDeclaration(declaration: KtAnnotated): Set<ClassId> {
        val annotationEntries = when (declaration) {
            is KtFile -> declaration.annotationEntries
            is KtDeclaration -> declaration.annotationEntries
            else -> error("Unexpected element of class ${declaration::class}")
        }

        return annotationEntries.mapNotNull { it.resolveAnnotationId() }.toSet()
    }

    /**
     * Examples of usage:
     *
     * - `Baz` -> `FqName("Baz")`
     * - `Bar.Baz` -> `FqName("Bar.Baz")`
     * - `foo.bar.Baz<A, B>` -> `FqName("foo.bar.Baz")`
     */
    private fun KtUserType.referencedFqName(): FqName? {
        val allTypes = generateSequence(this) { it.qualifier }.toList().asReversed()
        val allQualifiers = allTypes.map { it.referencedName ?: return null }

        return FqName.fromSegments(allQualifiers)
    }

    private fun KtAnnotationEntry.resolveAnnotationId(): ClassId? {
        return resolveAnnotationFqName()?.let { ClassId.topLevel(it) }
    }

    private fun KtAnnotationEntry.resolveAnnotationFqName(): FqName? {
        val annotationTypeElement = typeReference?.typeElement as? KtUserType
        val referencedName = annotationTypeElement?.referencedFqName() ?: return null

        // FIXME what happens with aliased imports? They are correctly reported by the annotation index
        if (referencedName.isRoot) return null

        if (!referencedName.parent().isRoot) {
            // we assume here that the annotation is used by its fully-qualified name
            return referencedName.takeIf { annotationActuallyExists(it) }
        }

        val candidates = getCandidatesFromImports(containingKtFile, referencedName.shortName())

        return candidates.fromExplicitImports.resolveToSingleName()
            ?: candidates.fromSamePackage.resolveToSingleName()
            ?: candidates.fromStarImports.resolveToSingleName()
    }

    /**
     * A set of places where the annotation can be possibly resolved.
     *
     * @param fromSamePackage A possible candidate from the same package. It is a single name, but it is wrapped into a set for consistency.
     * @param fromExplicitImports Candidates from the explicit, fully-qualified imports with matching short name.
     * @param fromStarImports Candidates from all star imports in the file; it is possible that the annotation goes from one of them.
     */
    private data class ResolveByImportsCandidates(
        val fromSamePackage: Set<FqName>,
        val fromExplicitImports: Set<FqName>,
        val fromStarImports: Set<FqName>,
    )

    private fun getCandidatesFromImports(file: KtFile, targetName: Name): ResolveByImportsCandidates {
        val starImports = mutableSetOf<FqName>()
        val explicitImports = mutableSetOf<FqName>()

        for (import in file.importDirectives) {
            val importedName = import.importedFqName ?: continue

            if (import.isAllUnder) {
                starImports += importedName.child(targetName)
            } else if (importedName.shortName() == targetName) {
                explicitImports += importedName
            }
        }

        val packageImport = file.packageFqName.child(targetName)

        return ResolveByImportsCandidates(setOf(packageImport), explicitImports, starImports)
    }

    private fun Set<FqName>.resolveToSingleName(): FqName? = singleOrNull { annotationActuallyExists(it) }

    private fun annotationActuallyExists(matchingImport: FqName): Boolean {
        val foundClasses = KotlinFullClassNameIndex.get(stubIndex, matchingImport.asString(), project, searchScope)
        return foundClasses.singleOrNull { it.isAnnotation() && it.isTopLevel() } != null
    }
}

/**
 * A declaration which is annotated with passed [KtAnnotationEntry].
 */
private val KtAnnotationEntry.annotationOwner: KtAnnotated?
    get() {
        val modifierListEntry = this.parent as? KtAnnotation ?: this
        val modifierList = modifierListEntry.parent as? KtModifierList

        return modifierList?.parent as? KtAnnotated
    }

