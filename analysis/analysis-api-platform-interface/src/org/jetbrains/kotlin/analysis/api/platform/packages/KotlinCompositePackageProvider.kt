/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.platform.packages

import org.jetbrains.kotlin.analysis.api.platform.KotlinCompositeProvider
import org.jetbrains.kotlin.analysis.api.platform.KotlinCompositeProviderFactory
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.TargetPlatform

/**
 * A composite [KotlinPackageProvider] which combines all packages provided by its child [providers]. It should be created with
 * [KotlinCompositePackageProvider.create].
 */
public class KotlinCompositePackageProvider private constructor(
    override val providers: List<KotlinPackageProvider>,
) : KotlinPackageProvider, KotlinCompositeProvider<KotlinPackageProvider> {
    override fun doesPackageExist(packageFqName: FqName, platform: TargetPlatform): Boolean {
        return providers.any { it.doesPackageExist(packageFqName, platform) }
    }

    override fun doesKotlinOnlyPackageExist(packageFqName: FqName): Boolean {
        return providers.any { it.doesKotlinOnlyPackageExist(packageFqName) }
    }

    override fun doesPlatformSpecificPackageExist(packageFqName: FqName, platform: TargetPlatform): Boolean {
        return providers.any { it.doesPlatformSpecificPackageExist(packageFqName, platform) }
    }

    override fun getSubpackageNames(packageFqName: FqName, platform: TargetPlatform): Set<Name> {
        return providers.flatMapTo(mutableSetOf()) { it.getSubpackageNames(packageFqName, platform) }
    }

    override fun getKotlinOnlySubpackageNames(packageFqName: FqName): Set<Name> {
        return providers.flatMapTo(mutableSetOf()) { it.getKotlinOnlySubpackageNames(packageFqName) }
    }

    override fun getPlatformSpecificSubpackageNames(packageFqName: FqName, platform: TargetPlatform): Set<Name> {
        return providers.flatMapTo(mutableSetOf()) { it.getPlatformSpecificSubpackageNames(packageFqName, platform) }
    }

    public companion object {
        public val factory: KotlinCompositeProviderFactory<KotlinPackageProvider> = KotlinCompositeProviderFactory(
            KotlinEmptyPackageProvider,
            ::KotlinCompositePackageProvider,
        )

        public fun create(providers: List<KotlinPackageProvider>): KotlinPackageProvider = factory.create(providers)
    }
}
