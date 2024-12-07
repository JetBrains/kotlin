/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.platform.declarations

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.api.platform.KotlinComposableProviderMerger
import org.jetbrains.kotlin.analysis.api.platform.KotlinPlatformComponent
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule

/**
 * [KotlinDeclarationProviderFactory] creates a scope-based [KotlinDeclarationProvider] which covers all non-generated, physical
 * declarations found in source files and possibly indices/stubs for libraries.
 *
 * The declaration provider created by [KotlinDeclarationProviderFactory] is considered the **main declaration provider** for the given
 * scope. Other kinds of declaration providers exist, but they usually cover generated declarations for edge cases.
 *
 * @see KotlinDeclarationProvider
 */
public interface KotlinDeclarationProviderFactory : KotlinPlatformComponent {
    public fun createDeclarationProvider(scope: GlobalSearchScope, contextualModule: KaModule?): KotlinDeclarationProvider

    public companion object {
        public fun getInstance(project: Project): KotlinDeclarationProviderFactory = project.service()
    }
}

/**
 * While the main declaration provider created via [createDeclarationProvider] is scope-based, there are other declaration providers which
 * are not, such as file-based declaration providers. Not all declarations provided by such declaration providers can be provided by the
 * main declaration provider, even if the correct scope is provided (such as a file-based scope). For example, the main declaration provider
 * may be based on an index which doesn't contain the declarations provided by file-based declaration providers.
 *
 * Hence, [KotlinDeclarationProvider]s cannot just be combined by combining the scopes of all declaration providers and calling
 * [createDeclarationProvider]. [KotlinDeclarationProviderMerger] should implement proper merging logic that takes these concerns into
 * account.
 *
 * The provider merger should consider merging scopes with [KotlinGlobalSearchScopeMerger][org.jetbrains.kotlin.analysis.api.platform.projectStructure.KotlinGlobalSearchScopeMerger]
 * if there is a useful implementation provided by the platform.
 */
public interface KotlinDeclarationProviderMerger : KotlinComposableProviderMerger<KotlinDeclarationProvider>, KotlinPlatformComponent {
    public companion object {
        public fun getInstance(project: Project): KotlinDeclarationProviderMerger = project.service()
    }
}

/**
 * Creates a [KotlinDeclarationProvider] providing symbols within the given [scope].
 *
 * The [contextualModule] is the module which contains the symbols to be provided, if applicable. The declaration provider may use the
 * contextual module to provide declarations differently, such as providing alternative declarations for an outsider module. Some
 * functionality such as package set computation may also depend on the contextual module, as the declaration provider may require
 * additional information not available in the [scope].
 */
public fun Project.createDeclarationProvider(scope: GlobalSearchScope, contextualModule: KaModule?): KotlinDeclarationProvider =
    KotlinDeclarationProviderFactory.getInstance(this).createDeclarationProvider(scope, contextualModule)

public fun Project.mergeDeclarationProviders(declarationProviders: List<KotlinDeclarationProvider>): KotlinDeclarationProvider =
    KotlinDeclarationProviderMerger.getInstance(this).merge(declarationProviders)
