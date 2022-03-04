/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.providers

import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiField
import com.intellij.psi.PsiMethod
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId

/**
 * A compiled declaration provider for a given scope. Can be created via [CompiledDeclarationProviderFactory].
 */
public abstract class CompiledDeclarationProvider {
    /**
     * Gets a collection of [PsiClass] by [ClassId]
     *
     * In IDE, this could be decompiled source from the decompiler, such as [KtClassOrObject] inside [KtClsFile] as [KtFile].
     * In standalone mode, this is simply [PsiClassStub]-based [PsiClass]
     *   that can be wrapped by [KtLightClassForDecompiledDeclaration],
     *   which is still a subtype of [PsiClass], if the result goes through LC conversion (UAST case).
     */
    public abstract fun getClassesByClassId(classId: ClassId): Collection<PsiClass>

    public abstract fun getProperties(callableId: CallableId): Collection<PsiField>
    public abstract fun getFunctions(callableId: CallableId): Collection<PsiMethod>
}

public abstract class CompiledDeclarationProviderFactory {
    public abstract fun createCompiledDeclarationProvider(searchScope: GlobalSearchScope): CompiledDeclarationProvider
}

public fun Project.createCompiledDeclarationProvider(searchScope: GlobalSearchScope): CompiledDeclarationProvider =
    ServiceManager.getService(this, CompiledDeclarationProviderFactory::class.java)
        .createCompiledDeclarationProvider(searchScope)
