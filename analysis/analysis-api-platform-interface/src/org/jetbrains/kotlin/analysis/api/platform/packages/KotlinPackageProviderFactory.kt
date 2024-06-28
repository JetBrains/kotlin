/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.platform.packages

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.api.platform.KotlinComposableProviderMerger
import org.jetbrains.kotlin.analysis.api.platform.KotlinPlatformComponent

/**
 * [KotlinPackageProviderFactory] creates a scope-based [KotlinPackageProvider] which covers all non-generated packages found in source
 * files and possibly indices/stubs for libraries.
 *
 * The package provider created by [KotlinPackageProviderFactory] is considered the **main package provider** for the given scope. Other
 * kinds of package providers exist, but they usually cover generated packages for edge cases.
 *
 * @see KotlinPackageProvider
 */
public interface KotlinPackageProviderFactory : KotlinPlatformComponent {
    public fun createPackageProvider(searchScope: GlobalSearchScope): KotlinPackageProvider

    public companion object {
        public fun getInstance(project: Project): KotlinPackageProviderFactory = project.service()
    }
}

/**
 * [KotlinPackageProviderMerger] allows merging multiple [KotlinPackageProvider]s into a more efficient package provider.
 *
 * Package providers should not be naively merged by combining scopes and calling [createPackageProvider], because there may be additional
 * package providers which do not operate based on scopes (e.g. resolve extension package providers).
 */
public interface KotlinPackageProviderMerger : KotlinComposableProviderMerger<KotlinPackageProvider>, KotlinPlatformComponent {
    public companion object {
        public fun getInstance(project: Project): KotlinPackageProviderMerger = project.service()
    }
}

public fun Project.createPackageProvider(searchScope: GlobalSearchScope): KotlinPackageProvider =
    KotlinPackageProviderFactory.getInstance(this).createPackageProvider(searchScope)

public fun Project.mergePackageProviders(packageProviders: List<KotlinPackageProvider>): KotlinPackageProvider =
    KotlinPackageProviderMerger.getInstance(this).merge(packageProviders)
