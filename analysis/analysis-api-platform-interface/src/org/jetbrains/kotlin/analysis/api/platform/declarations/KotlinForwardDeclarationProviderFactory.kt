/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.platform.declarations

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analysis.api.platform.KotlinOptionalPlatformComponent
import org.jetbrains.kotlin.analysis.project.structure.KtModule

/**
 * Declaration provider factory for Kotlin/Native forward declarations.
 *
 * The declarations from the provider are used as a source **representation** for forward declaration symbols.
 * The special provider is necessary because forward declarations are mere qualified names by themselves.
 * It is a responsibility of the implementor to make the source representation correct.
 * Symbol's properties are not affected by its source representation.
 *
 * Implementations should be consistent with their corresponding [KotlinForwardDeclarationsPackageProviderFactory][org.jetbrains.kotlin.analysis.api.platform.packages.KotlinForwardDeclarationsPackageProviderFactory]
 * implementation.
 */
public abstract class KotlinForwardDeclarationProviderFactory : KotlinOptionalPlatformComponent {
    /**
     * Create a Kotlin/Native declaration provider for [ktModule].
     *
     * Generally, only Kotlin/Native KLIB libraries can declare forward declarations.
     * For other types of [KtModule]s the provider normally shouldn't be created.
     *
     * @return a declaration provider for [ktModule] or `null` if the module cannot contain forward declarations
     */
    public abstract fun createDeclarationProvider(ktModule: KtModule): KotlinDeclarationProvider?

    public companion object {
        public fun getInstance(project: Project): KotlinForwardDeclarationProviderFactory? =
            project.getService(KotlinForwardDeclarationProviderFactory::class.java)
    }
}

/**
 * Create a declaration provider for [ktModule]'s forward declarations or `null` if the module cannot contain forward declarations.
 *
 * @see [KotlinForwardDeclarationProviderFactory]
 */
public fun Project.createForwardDeclarationProvider(ktModule: KtModule): KotlinDeclarationProvider? =
    KotlinForwardDeclarationProviderFactory.getInstance(this)?.createDeclarationProvider(ktModule)
