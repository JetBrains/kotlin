/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.api.services

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analysis.providers.KotlinSealedInheritorsProvider
import org.jetbrains.kotlin.analysis.providers.KotlinSealedInheritorsProviderFactory
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.SealedClassInheritorsProvider
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.psi.KtClass

interface LLSealedInheritorsProviderFactory {
    /**
     * Creates a [SealedClassInheritorsProvider] for a session. A new [SealedClassInheritorsProvider] instance should be created on every
     * call.
     */
    fun createSealedInheritorsProvider(): SealedClassInheritorsProvider
}

/**
 * Creates a [SealedClassInheritorsProvider] for a session.
 *
 * Requires either [LLSealedInheritorsProviderFactory] or [KotlinSealedInheritorsProviderFactory] to be registered as a project service.
 */
internal fun createSealedInheritorsProvider(project: Project): SealedClassInheritorsProvider =
    createFromLLProvider(project)
        ?: createFromAnalysisApiProvider(project)
        ?: error(
            """
            Expected one of the following services to be registered with $project:
                - ${LLSealedInheritorsProviderFactory::class.simpleName}
                - ${KotlinSealedInheritorsProviderFactory::class.simpleName}
            """.trimIndent()
        )

private fun createFromLLProvider(project: Project): SealedClassInheritorsProvider? =
    project.getService(LLSealedInheritorsProviderFactory::class.java)?.createSealedInheritorsProvider()

private fun createFromAnalysisApiProvider(project: Project): SealedClassInheritorsProvider? =
    KotlinSealedInheritorsProviderFactory.getInstance(project)
        ?.createSealedInheritorsProvider()
        ?.let(::LLSealedInheritorsProviderByAnalysisApiProvider)

private class LLSealedInheritorsProviderByAnalysisApiProvider(
    private val provider: KotlinSealedInheritorsProvider,
) : SealedClassInheritorsProvider() {
    override fun getSealedClassInheritors(firClass: FirRegularClass): List<ClassId> {
        val ktClass = firClass.psi as? KtClass ?: return emptyList()
        return provider.getSealedInheritors(ktClass)
    }
}
