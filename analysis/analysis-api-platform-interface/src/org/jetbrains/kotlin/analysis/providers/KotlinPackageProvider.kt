/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.providers

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.TargetPlatform

/**
 * Provides information about packages that are visible to Kotlin in the given scope. Can be constructed via [KotlinPackageProviderFactory].
 * The FIR compiler calls [doesKotlinOnlyPackageExist]  very often, so the implementations should consider caching the results.
 */
public abstract class KotlinPackageProvider : KotlinComposableProvider {
    /**
     * Checks if a package with given [FqName] exists in current [GlobalSearchScope] with a view from a given [platform].
     *
     * This includes Kotlin packages as well as platform-specific (i.e., JVM packages) that match the [platform].
     * Generally, the result is equal to [doesKotlinOnlyPackageExist] || [doesPlatformSpecificPackageExist].
     */
    public abstract fun doesPackageExist(packageFqName: FqName, platform: TargetPlatform): Boolean

    /**
     * Checks if a package with a given [FqName] exists in the current [GlobalSearchScope].
     *
     * The package should contain Kotlin declarations inside.
     *
     * Note that for Kotlin, a package doesn't need to correspond to a directory structure like in Java.
     * So, a package [FqName] is determined by a Kotlin file package directive.
     */
    public abstract fun doesKotlinOnlyPackageExist(packageFqName: FqName): Boolean

    /**
     * Checks if a platform-specific (e.g., Java packages for Kotlin/JVM) package with [FqName] exists in the current [GlobalSearchScope].
     */
    public abstract fun doesPlatformSpecificPackageExist(packageFqName: FqName, platform: TargetPlatform): Boolean


    /**
     * Returns the list of subpackages for a given package, which satisfies [nameFilter].
     *
     * The returned sub-package list contains sub-packages visible to Kotlin. (e.g., for Kotlin/JVM, it should include Java packages)
     *
     * Generally, the result is equal to [getKotlinOnlySubPackagesFqNames] union with [getPlatformSpecificSubPackagesFqNames]
     */
    public abstract fun getSubPackageFqNames(
        packageFqName: FqName,
        platform: TargetPlatform,
        nameFilter: (Name) -> Boolean
    ): Set<Name>

    /**
     * Returns the list of subpackages for a given package, which satisfies [nameFilter].
     *
     * The returned sub-package list contains all packages with some Kotlin declarations inside.
     */
    public abstract fun getKotlinOnlySubPackagesFqNames(packageFqName: FqName, nameFilter: (Name) -> Boolean): Set<Name>

    /**
     * Returns the platform-specific (e.g., Java packages for Kotlin/JVM) list of subpackages for a given package, which satisfies [nameFilter].
     *
     * The returned sub-package list contains sub-packages visible to Kotlin. (e.g., for Kotlin/JVM, it should include Java packages)
     */
    public abstract fun getPlatformSpecificSubPackagesFqNames(
        packageFqName: FqName,
        platform: TargetPlatform,
        nameFilter: (Name) -> Boolean
    ): Set<Name>
}

public abstract class KotlinPackageProviderFactory {
    public abstract fun createPackageProvider(searchScope: GlobalSearchScope): KotlinPackageProvider
}

/**
 * [KotlinPackageProviderMerger] allows merging multiple [KotlinPackageProvider]s into a more efficient package provider.
 *
 * Package providers should not be naively merged by combining scopes and calling [createPackageProvider], because there may be additional
 * package providers which do not operate based on scopes (e.g. resolve extension package providers).
 */
public abstract class KotlinPackageProviderMerger : KotlinComposableProviderMerger<KotlinPackageProvider> {
    public companion object {
        public fun getInstance(project: Project): KotlinPackageProviderMerger = project.getService(KotlinPackageProviderMerger::class.java)
    }
}

public fun Project.createPackageProvider(searchScope: GlobalSearchScope): KotlinPackageProvider =
    this.getService(KotlinPackageProviderFactory::class.java)
        .createPackageProvider(searchScope)

public fun Project.mergePackageProviders(packageProviders: List<KotlinPackageProvider>): KotlinPackageProvider =
    KotlinPackageProviderMerger.getInstance(this).merge(packageProviders)
