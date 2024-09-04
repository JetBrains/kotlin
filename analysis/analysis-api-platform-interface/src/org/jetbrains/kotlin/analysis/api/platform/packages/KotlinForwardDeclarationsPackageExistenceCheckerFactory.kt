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
 * [KotlinForwardDeclarationsPackageExistenceCheckerFactory] creates [KotlinPackageProvider]s for Kotlin/Native forward declarations.
 *
 * Implementations should be consistent with their corresponding [KotlinForwardDeclarationProviderFactory][org.jetbrains.kotlin.analysis.api.platform.declarations.KotlinForwardDeclarationProviderFactory]
 * implementation.
 */
public interface KotlinForwardDeclarationsPackageExistenceCheckerFactory : KotlinOptionalPlatformComponent {
    /**
     * Creates a package provider for Kotlin/Native forward declaration packages in [module], or `null` if the module cannot contain forward
     * declarations.
     */
    public fun createPackageExistenceChecker(module: KaModule): KotlinPackageExistenceChecker?

    public companion object {
        public fun getInstance(project: Project): KotlinForwardDeclarationsPackageExistenceCheckerFactory? = project.serviceOrNull()
    }
}

/**
 * Creates a package provider for Kotlin/Native forward declaration packages in [module], or `null` if the module cannot contain forward
 * declarations.
 *
 * @see KotlinForwardDeclarationsPackageExistenceCheckerFactory
 */
public fun Project.createForwardDeclarationsPackageExistenceChecker(module: KaModule): KotlinPackageExistenceChecker? =
    KotlinForwardDeclarationsPackageExistenceCheckerFactory.getInstance(this)?.createPackageExistenceChecker(module)
