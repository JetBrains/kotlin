/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.providers.impl

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.providers.KotlinAnnotationsResolver
import org.jetbrains.kotlin.analysis.providers.KotlinAnnotationsResolverFactory
import org.jetbrains.kotlin.analysis.providers.KotlinDeclarationProvider
import org.jetbrains.kotlin.analysis.providers.createDeclarationProvider
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.utils.filterIsInstanceAnd

/**
 * This implementation works only for FQN annotations usages (`@foo.Bar` instead of `@Bar`).
 * It does not perform the full resolve of the annotation call, but it checks whether the annotation
 * with such FQN is present in the [scope] with [KotlinDeclarationProvider].
 *
 * Required for and used only in the test infrastructure.
 */
private class KotlinStaticAnnotationsResolver(
    private val project: Project,
    ktFiles: Collection<KtFile>,
    scope: GlobalSearchScope
) : KotlinAnnotationsResolver {
    private val declarationProvider: KotlinDeclarationProvider by lazy {
        project.createDeclarationProvider(scope, contextualModule = null)
    }

    private val filesInScope = ktFiles.filter { scope.contains(it.virtualFile) }

    private val allDeclarations: List<KtDeclaration> by lazy {
        val result = mutableListOf<KtDeclaration>()

        val visitor = declarationRecursiveVisitor visit@{
            val isLocal = when (it) {
                is KtClassOrObject -> it.isLocal
                is KtFunction -> it.isLocal
                is KtProperty -> it.isLocal
                else -> return@visit
            }

            if (!isLocal) {
                result += it
            }
        }

        filesInScope.forEach { it.accept(visitor) }

        result
    }

    override fun declarationsByAnnotation(queriedAnnotation: ClassId): Set<KtAnnotated> {
        return allDeclarations.asSequence()
            .filter { queriedAnnotation in annotationsOnDeclaration(it) }
            .toSet()
    }

    override fun annotationsOnDeclaration(declaration: KtAnnotated): Set<ClassId> {
        return declaration.annotationEntries.asSequence()
            .mapNotNull { it.typeReference?.text }
            .map { ClassId.topLevel(FqName(it)) }
            .filter { it.resolveToAnnotation() != null }
            .toSet()
    }

    private fun ClassId.resolveToAnnotation(): KtClass? {
        val classes = declarationProvider.getAllClassesByClassId(this)
        val annotations = classes.filterIsInstanceAnd<KtClass> { it.isAnnotation() }

        return annotations.singleOrNull()
    }
}

public class KotlinStaticAnnotationsResolverFactory(
    private val project: Project,
    private val files: Collection<KtFile>,
) : KotlinAnnotationsResolverFactory {
    override fun createAnnotationResolver(searchScope: GlobalSearchScope): KotlinAnnotationsResolver {
        return KotlinStaticAnnotationsResolver(project, files, searchScope)
    }
}