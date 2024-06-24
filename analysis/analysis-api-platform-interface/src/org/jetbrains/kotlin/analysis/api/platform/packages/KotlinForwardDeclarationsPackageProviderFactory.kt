/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.platform.packages

import com.intellij.openapi.components.serviceOrNull
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analysis.api.platform.KotlinOptionalPlatformComponent
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule

/**
 * Package provider factory for the Kotlin/Native forward declarations symbol provider.
 *
 * Implementations should be consistent with their corresponding [KotlinForwardDeclarationProviderFactory][org.jetbrains.kotlin.analysis.api.platform.declarations.KotlinForwardDeclarationProviderFactory]
 * implementation.
 */
public abstract class KotlinForwardDeclarationsPackageProviderFactory : KotlinOptionalPlatformComponent {
    /**
     * Create a package provider for Kotlin/Native forward declaration packages in this [module].
     *
     * @return a package provider for [module] or `null` if the module cannot contain forward declarations
     */
    public abstract fun createPackageProvider(module: KaModule): KotlinPackageProvider?

    public companion object {
        public fun getInstance(project: Project): KotlinForwardDeclarationsPackageProviderFactory? = project.serviceOrNull()
    }
}

/**
 * Create a package provider for [module]'s forward declarations or `null` if the module cannot contain forward declarations.
 *
 * @see [KotlinForwardDeclarationsPackageProviderFactory]
 */
public fun Project.createForwardDeclarationsPackageProvider(module: KaModule): KotlinPackageProvider? =
    KotlinForwardDeclarationsPackageProviderFactory.getInstance(this)?.createPackageProvider(module)
