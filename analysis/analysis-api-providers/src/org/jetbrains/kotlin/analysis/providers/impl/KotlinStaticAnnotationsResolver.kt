/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.providers.impl

import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.providers.KotlinAnnotationsResolver
import org.jetbrains.kotlin.analysis.providers.KotlinAnnotationsResolverFactory
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*

/**
 * This implementation works only for fully-qualified annotations usages, and does not check if they resolve somewhere.
 *
 * Required for the test infrastructure.
 */
private class KotlinStaticAnnotationsResolver(
    ktFiles: Collection<KtFile>,
    scope: GlobalSearchScope
) : KotlinAnnotationsResolver {
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
            .toSet()
    }
}

public class KotlinStaticAnnotationsResolverFactory(private val files: Collection<KtFile>) : KotlinAnnotationsResolverFactory {
    override fun createAnnotationResolver(searchScope: GlobalSearchScope): KotlinAnnotationsResolver {
        return KotlinStaticAnnotationsResolver(files, searchScope)
    }
}