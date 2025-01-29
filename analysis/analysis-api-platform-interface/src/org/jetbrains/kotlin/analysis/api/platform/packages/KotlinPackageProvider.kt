/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.platform.packages

import org.jetbrains.kotlin.analysis.api.platform.KotlinComposableProvider
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.TargetPlatform

/**
 * [KotlinPackageProvider] provides information about packages that are visible to Kotlin in a certain context. The provider covers not only
 * packages with Kotlin declarations, but also packages with declarations that match a specified [TargetPlatform]. For example, packages
 * with Java declarations on the JVM platform.
 *
 * Package providers usually don't cover the whole project, but rather a restricted context. This usually means being limited to a specific
 * [scope][com.intellij.psi.search.GlobalSearchScope], but the details depend on the kind of package provider.
 *
 * The main kind of package providers is created via [KotlinPackageProviderFactory] and it is scope-based, but there are other kinds of
 * package providers, such as those created by [KotlinForwardDeclarationsPackageProviderFactory].
 *
 * Package providers are critical for performance, so implementations should cache results.
 *
 * ### Lifetime
 *
 * [KotlinPackageProvider] has the same lifetime guarantees as [KotlinDeclarationProvider][org.jetbrains.kotlin.analysis.api.platform.declarations.KotlinDeclarationProvider].
 */
public interface KotlinPackageProvider : KotlinComposableProvider {
    /**
     * Checks if a package named [packageFqName] exists in the context of the given [platform]. This includes Kotlin packages as well as
     * [platform]-specific packages.
     *
     * Generally, the result is equal to: [doesKotlinOnlyPackageExist] || [doesPlatformSpecificPackageExist].
     */
    public fun doesPackageExist(packageFqName: FqName, platform: TargetPlatform): Boolean

    /**
     * Checks if a package named [packageFqName] exists. The package should contain Kotlin declarations.
     *
     * Note that for Kotlin, a package doesn't need to correspond to a directory structure like in Java. So, a file's package [FqName] is
     * determined by the `package` directive.
     */
    public fun doesKotlinOnlyPackageExist(packageFqName: FqName): Boolean

    /**
     * Checks if a package named [packageFqName] exists. The package should contain [platform]-specific declarations.
     */
    public fun doesPlatformSpecificPackageExist(packageFqName: FqName, platform: TargetPlatform): Boolean

    /**
     * Returns the list of subpackages of a given package.
     *
     * The returned subpackage list contains both Kotlin and [platform]-specific subpackages (e.g., for Kotlin/JVM, it should include Java
     * packages).
     *
     * Generally, the result is equal to: [getKotlinOnlySubpackageNames] union [getPlatformSpecificSubpackageNames].
     */
    public fun getSubpackageNames(packageFqName: FqName, platform: TargetPlatform): Set<Name>

    /**
     * Returns the list of subpackages of a given package which contain Kotlin declarations.
     */
    public fun getKotlinOnlySubpackageNames(packageFqName: FqName): Set<Name>

    /**
     * Returns the list of subpackages of a given package which contain [platform]-specific declarations.
     */
    public fun getPlatformSpecificSubpackageNames(
        packageFqName: FqName,
        platform: TargetPlatform,
    ): Set<Name>
}
