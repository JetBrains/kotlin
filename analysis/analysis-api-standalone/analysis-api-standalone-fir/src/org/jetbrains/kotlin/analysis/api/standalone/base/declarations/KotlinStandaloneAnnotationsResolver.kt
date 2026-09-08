/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.standalone.base.declarations

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.platform.declarations.KotlinAnnotationsResolver
import org.jetbrains.kotlin.analysis.api.platform.declarations.KotlinAnnotationsResolverFactory
import org.jetbrains.kotlin.analysis.api.platform.declarations.KotlinDeclarationProvider
import org.jetbrains.kotlin.analysis.api.platform.declarations.createDeclarationProvider
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.parentOrNull
import org.jetbrains.kotlin.psi.*

/**
 * This implementation doesn't perform the full resolve of the annotation call,
 * but it checks whether the annotation with such FQN is present in the [scope] with [KotlinDeclarationProvider].
 */
private class KotlinStandaloneAnnotationsResolver(
    private val project: Project,
    ktFiles: Collection<KtFile>,
    scope: GlobalSearchScope,
) : KotlinAnnotationsResolver {
    private val declarationProvider: KotlinDeclarationProvider by lazy {
        project.createDeclarationProvider(scope, contextualModule = null)
    }

    private val filesInScope = ktFiles.filter { scope.contains(it.virtualFile) }

    private val allDeclarations: List<KtDeclaration> by lazy {
        val result = mutableListOf<KtDeclaration>()

        val visitor = declarationRecursiveVisitor {
            result += it
        }

        filesInScope.forEach { it.accept(visitor) }

        result
    }

    override fun declarationsByAnnotation(annotationClassId: ClassId): Set<KtAnnotated> {
        return allDeclarations.asSequence()
            .filter { annotationClassId in annotationsOnDeclaration(it) }
            .toSet()
    }

    override fun annotationsOnDeclaration(declaration: KtAnnotated): Set<ClassId> = declaration.annotationEntries
        .asSequence()
        .flatMap { it.typeReference?.resolveAnnotationClassIds().orEmpty() }
        .toSet()

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

    private fun KtTypeReference.resolveAnnotationClassIds(
        candidates: MutableSet<ClassId> = mutableSetOf(),
        visitedClassIds: MutableSet<ClassId> = mutableSetOf(),
    ): Set<ClassId> {
        val annotationTypeElement = typeElement as? KtUserType
        val referencedName = annotationTypeElement?.referencedFqName() ?: return emptySet()
        if (referencedName.isRoot) return emptySet()

        if (!referencedName.parent().isRoot) {
            // we assume here that the annotation is used by its fully-qualified name
            referencedName.resolveToClassIds(candidates, visitedClassIds)
            return candidates
        }

        val targetName = referencedName.shortName()
        for (import in containingKtFile.importDirectives) {
            val importedName = import.importedFqName ?: continue
            when {
                import.isAllUnder -> importedName.child(targetName).resolveToClassIds(candidates, visitedClassIds)
                importedName.shortName() == targetName -> importedName.resolveToClassIds(candidates, visitedClassIds)
            }
        }

        containingKtFile.packageFqName.child(targetName).resolveToClassIds(candidates, visitedClassIds)
        return candidates
    }

    private fun FqName.toClassIdSequence(): Sequence<ClassId> {
        var currentName = shortNameOrSpecial()
        if (currentName.isSpecial) return emptySequence()
        var currentParent = parentOrNull() ?: return emptySequence()
        var currentRelativeName = currentName.asString()

        return sequence {
            while (true) {
                yield(ClassId(currentParent, FqName(currentRelativeName), isLocal = false))
                currentName = currentParent.shortNameOrSpecial()
                if (currentName.isSpecial) break
                currentParent = currentParent.parentOrNull() ?: break
                currentRelativeName = "${currentName.asString()}.$currentRelativeName"
            }
        }
    }

    fun FqName.resolveToClassIds(to: MutableSet<ClassId>, visitedClassIds: MutableSet<ClassId>) {
        for (classId in toClassIdSequence()) {
            // The same class id may be provided by several declarations, e.g., by a source declaration and by a library
            // declaration shadowed by it. The resolver is allowed to report false positives, so every declaration
            // contributes instead of only an unambiguous one
            //
            // Type aliases may form a cycle in erroneous code, so every class id is expanded at most once.
            // Expanding it again cannot contribute anything new anyway
            if (visitedClassIds.add(classId)) {
                for (typeAlias in declarationProvider.getAllTypeAliasesByClassId(classId)) {
                    typeAlias.getTypeReference()?.resolveAnnotationClassIds(to, visitedClassIds)
                }
            }

            val classes = declarationProvider.getAllClassesByClassId(classId)
            if (classes.any { it is KtClass && it.isAnnotation() }) {
                to += classId
            }
        }
    }
}

@KaImplementationDetail
class KotlinStandaloneAnnotationsResolverFactory(
    private val project: Project,
    private val files: Collection<KtFile>,
) : KotlinAnnotationsResolverFactory {
    override fun createAnnotationResolver(searchScope: GlobalSearchScope): KotlinAnnotationsResolver {
        return KotlinStandaloneAnnotationsResolver(project, files, searchScope)
    }
}
