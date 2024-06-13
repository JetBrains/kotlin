/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.platform.declarations

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.api.platform.KotlinComposableProviderMerger
import org.jetbrains.kotlin.analysis.api.platform.KotlinPlatformComponent
import org.jetbrains.kotlin.analysis.project.structure.KtModule

/**
 * @see KotlinDeclarationProvider
 */
public abstract class KotlinDeclarationProviderFactory : KotlinPlatformComponent {
    public abstract fun createDeclarationProvider(scope: GlobalSearchScope, contextualModule: KtModule?): KotlinDeclarationProvider

    public companion object {
        public fun getInstance(project: Project): KotlinDeclarationProviderFactory =
            project.getService(KotlinDeclarationProviderFactory::class.java)
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
 */
public abstract class KotlinDeclarationProviderMerger : KotlinComposableProviderMerger<KotlinDeclarationProvider>, KotlinPlatformComponent {
    public companion object {
        public fun getInstance(project: Project): KotlinDeclarationProviderMerger =
            project.getService(KotlinDeclarationProviderMerger::class.java)
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
public fun Project.createDeclarationProvider(scope: GlobalSearchScope, contextualModule: KtModule?): KotlinDeclarationProvider =
    KotlinDeclarationProviderFactory.getInstance(this).createDeclarationProvider(scope, contextualModule)

public fun Project.mergeDeclarationProviders(declarationProviders: List<KotlinDeclarationProvider>): KotlinDeclarationProvider =
    KotlinDeclarationProviderMerger.getInstance(this).merge(declarationProviders)
