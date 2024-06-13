/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.platform.packages

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.api.platform.KotlinComposableProviderMerger
import org.jetbrains.kotlin.analysis.api.platform.KotlinPlatformComponent

public abstract class KotlinPackageProviderFactory : KotlinPlatformComponent {
    public abstract fun createPackageProvider(searchScope: GlobalSearchScope): KotlinPackageProvider
}

/**
 * [KotlinPackageProviderMerger] allows merging multiple [KotlinPackageProvider]s into a more efficient package provider.
 *
 * Package providers should not be naively merged by combining scopes and calling [createPackageProvider], because there may be additional
 * package providers which do not operate based on scopes (e.g. resolve extension package providers).
 */
public abstract class KotlinPackageProviderMerger : KotlinComposableProviderMerger<KotlinPackageProvider>, KotlinPlatformComponent {
    public companion object {
        public fun getInstance(project: Project): KotlinPackageProviderMerger = project.getService(KotlinPackageProviderMerger::class.java)
    }
}

public fun Project.createPackageProvider(searchScope: GlobalSearchScope): KotlinPackageProvider =
    this.getService(KotlinPackageProviderFactory::class.java)
        .createPackageProvider(searchScope)

public fun Project.mergePackageProviders(packageProviders: List<KotlinPackageProvider>): KotlinPackageProvider =
    KotlinPackageProviderMerger.getInstance(this).merge(packageProviders)
