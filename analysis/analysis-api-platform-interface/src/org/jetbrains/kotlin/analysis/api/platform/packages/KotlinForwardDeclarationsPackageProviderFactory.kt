/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.platform.packages

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analysis.api.platform.KotlinOptionalPlatformComponent
import org.jetbrains.kotlin.analysis.project.structure.KtModule

/**
 * Package provider factory for the Kotlin/Native forward declarations symbol provider.
 *
 * Implementations should be consistent with their corresponding [KotlinForwardDeclarationProviderFactory][org.jetbrains.kotlin.analysis.api.platform.declarations.KotlinForwardDeclarationProviderFactory]
 * implementation.
 */
public abstract class KotlinForwardDeclarationsPackageProviderFactory : KotlinOptionalPlatformComponent {
    /**
     * Create a package provider for Kotlin/Native forward declaration packages in this [ktModule].
     *
     * @return a package provider for [ktModule] or `null` if the module cannot contain forward declarations
     */
    public abstract fun createPackageProvider(ktModule: KtModule): KotlinPackageProvider?

    public companion object {
        public fun getInstance(project: Project): KotlinForwardDeclarationsPackageProviderFactory? =
            project.getService(KotlinForwardDeclarationsPackageProviderFactory::class.java)
    }
}

/**
 * Create a package provider for [ktModule]'s forward declarations or `null` if the module cannot contain forward declarations.
 *
 * @see [KotlinForwardDeclarationsPackageProviderFactory]
 */
public fun Project.createForwardDeclarationsPackageProvider(ktModule: KtModule): KotlinPackageProvider? =
    KotlinForwardDeclarationsPackageProviderFactory.getInstance(this)?.createPackageProvider(ktModule)
