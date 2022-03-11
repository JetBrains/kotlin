/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.providers

import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtProperty

/**
 * A compiled declaration provider for a given scope. Can be created via [KotlinCompiledDeclarationProviderFactory].
 */
public abstract class KotlinCompiledDeclarationProvider {
    /**
     * Gets a collection of [KtClassOrObject] by [ClassId]
     *
     * In IDE, this could be decompiled source from the decompiler, such as [KtClassOrObject] inside [KtClsFile] as [KtFile].
     * In standalone mode, this is [KtClassOrObject] from [KotlinClassStubImpl].
     */
    public abstract fun getClassesByClassId(classId: ClassId): Collection<KtClassOrObject>

    public abstract fun getTopLevelProperties(callableId: CallableId): Collection<KtProperty>
    public abstract fun getTopLevelFunctions(callableId: CallableId): Collection<KtNamedFunction>
}

public abstract class KotlinCompiledDeclarationProviderFactory {
    public abstract fun createCompiledDeclarationProvider(searchScope: GlobalSearchScope): KotlinCompiledDeclarationProvider
}

public fun Project.createCompiledDeclarationProvider(searchScope: GlobalSearchScope): KotlinCompiledDeclarationProvider =
    ServiceManager.getService(this, KotlinCompiledDeclarationProviderFactory::class.java)
        .createCompiledDeclarationProvider(searchScope)
