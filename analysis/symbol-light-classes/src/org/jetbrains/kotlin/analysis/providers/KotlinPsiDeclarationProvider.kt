/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.providers

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMember
import com.intellij.psi.PsiMethod
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId

/**
 * A [PsiMember] declaration provider for a given scope. Can be created via [KotlinPsiDeclarationProviderFactory].
 */
abstract class KotlinPsiDeclarationProvider {
    /**
     * Gets a collection of [PsiClass] by [ClassId]
     *
     * In standalone mode, this is simply [PsiClassStub]-based [PsiClass]
     */
    abstract fun getClassesByClassId(classId: ClassId): Collection<PsiClass>

    abstract fun getProperties(callableId: CallableId): Collection<PsiMember>
    abstract fun getFunctions(callableId: CallableId): Collection<PsiMethod>
}

abstract class KotlinPsiDeclarationProviderFactory {
    abstract fun createPsiDeclarationProvider(searchScope: GlobalSearchScope): KotlinPsiDeclarationProvider
}

fun Project.createPsiDeclarationProvider(searchScope: GlobalSearchScope): KotlinPsiDeclarationProvider? =
    // TODO: avoid using fail-safe service loading once the factory has an easy-to-register ctor.
    getServiceIfCreated(KotlinPsiDeclarationProviderFactory::class.java)
        ?.createPsiDeclarationProvider(searchScope)
