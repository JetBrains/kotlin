/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.project.structure

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement

public abstract class ProjectStructureProvider {
    /**
     * Returns a [KtModule] for a given [element] in context of the [contextualModule].
     */
    public abstract fun getModule(element: PsiElement, contextualModule: KtModule?): KtModule

    companion object {
        public fun getInstance(project: Project): ProjectStructureProvider {
            return project.getService(ProjectStructureProvider::class.java)
        }

        public fun getModule(element: PsiElement, contextualModule: KtModule?): KtModule {
            return getInstance(element.project).getModule(element, contextualModule)
        }
    }
}

/**
 * For a given [PsiElement] get a [KtModule] to which [PsiElement] belongs.
 * @param project [Project] which contains current [PsiElement]. `PsiElement.project` may be a heavy operation as it includes PSI tree traversal. So, when a [Project] is  already available, it is better to pass it explicitly
 */
@Deprecated(
    "Use 'ProjectStructureProvider' directly",
    replaceWith = ReplaceWith("ProjectStructureProvider.getModule(element, contextualModule)"),
    level = DeprecationLevel.ERROR
)
public fun PsiElement.getKtModule(project: Project = this.project): KtModule =
    project.getService(ProjectStructureProvider::class.java)
        .getModule(this, null)

/**
 * For a given [PsiElement] get a [KtModule] to which [PsiElement] belongs.
 * @return [KtModule] of type [M] if `result <: M`, [java.lang.ClassCastException] otherwise
 * @param project [Project] which contains current [PsiElement]. `PsiElement.project` may be a heavy operation as it includes PSI tree traversal. So, when a [Project] is  already available, it is better to pass it explicitly
 */
@Deprecated(
    "Use 'ProjectStructureProvider' directly",
    replaceWith = ReplaceWith("ProjectStructureProvider.getModule(element, contextualModule)"),
    level = DeprecationLevel.ERROR
)
public inline fun <reified M : KtModule> PsiElement.getKtModuleOfType(project: Project = this.project): M {
    @Suppress("DEPRECATION_ERROR")
    return getKtModule(project) as M
}

/**
 * For a given [PsiElement] get a [KtModule] to which [PsiElement] belongs.
 * @return [KtModule] of type [M] if `result <: M`, `null` otherwise
 * @param project [Project] which contains current [PsiElement]. `PsiElement.project` may be a heavy operation as it includes PSI tree traversal. So, when a [Project] is  already available, it is better to pass it explicitly
 */
@Deprecated(
    "Use 'ProjectStructureProvider' directly",
    replaceWith = ReplaceWith("ProjectStructureProvider.getModule(element, contextualModule)"),
    level = DeprecationLevel.ERROR
)
public inline fun <reified M : KtModule> PsiElement.getKtModuleOfTypeSafe(project: Project = this.project): M? {
    @Suppress("DEPRECATION_ERROR")
    return getKtModule(project) as? M
}
