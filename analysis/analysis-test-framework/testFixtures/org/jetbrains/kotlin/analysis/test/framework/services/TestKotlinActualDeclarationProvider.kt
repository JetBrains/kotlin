/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.framework.services

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.impl.base.util.callableId
import org.jetbrains.kotlin.analysis.api.platform.declarations.createDeclarationProvider
import org.jetbrains.kotlin.analysis.api.platform.projectStructure.KotlinActualDeclarationProvider
import org.jetbrains.kotlin.analysis.api.platform.projectStructure.KotlinProjectStructureProvider
import org.jetbrains.kotlin.platform.isCommon
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtConstructor
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.allConstructors
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject
import org.jetbrains.kotlin.psi.psiUtil.isExpectDeclaration

class TestKotlinActualDeclarationProvider(private val project: Project) : KotlinActualDeclarationProvider {
    private val projectStructureProvider by lazy { KotlinProjectStructureProvider.getInstance(project) }

    override fun getActualDeclarations(declaration: KtDeclaration): Sequence<KtDeclaration> {
        if (!declaration.isExpectDeclaration()) {
            return emptySequence()
        }

        val module = projectStructureProvider.getModule(declaration, useSiteModule = null)
        if (!module.targetPlatform.isCommon()) {
            return emptySequence()
        }

        val implementingModules = projectStructureProvider.getImplementingModules(module)

        return sequence {
            for (implementingModule in implementingModules) {
                val declarationProvider = project.createDeclarationProvider(implementingModule.contentScope, contextualModule = null)

                val candidateDeclarations: Collection<KtDeclaration>? = when (declaration) {
                    is KtNamedFunction -> {
                        val callableId = declaration.callableId
                        if (callableId != null) declarationProvider.getTopLevelFunctions(callableId) else emptyList()
                    }
                    is KtConstructor<*> -> {
                        val containingClassId = declaration.containingClassOrObject?.getClassId()
                        if (containingClassId != null) {
                            val candidateClasses = declarationProvider.getAllClassesByClassId(containingClassId)
                            candidateClasses.flatMap { it.allConstructors }
                        } else {
                            emptyList()
                        }
                    }
                    is KtProperty -> {
                        val callableId = declaration.callableId
                        if (callableId != null) declarationProvider.getTopLevelProperties(callableId) else emptyList()
                    }
                    is KtClassOrObject -> {
                        val classId = declaration.getClassId()
                        if (classId != null) declarationProvider.getAllClassesByClassId(classId) else emptyList()
                    }
                    else -> null
                }

                // Iterating over all declarations without checking their signatures is certainly not the most effective way of
                // implementing the provider, but it's enough for testing purposes.
                for (candidateDeclaration in candidateDeclarations.orEmpty()) {
                    val isMatching = analyze(candidateDeclaration) {
                        val expectDeclarations = candidateDeclaration.symbol.getExpectsForActual()
                        expectDeclarations.any { declaration == it.psi }
                    }

                    if (isMatching) {
                        yield(candidateDeclaration)
                    }
                }
            }
        }
    }
}