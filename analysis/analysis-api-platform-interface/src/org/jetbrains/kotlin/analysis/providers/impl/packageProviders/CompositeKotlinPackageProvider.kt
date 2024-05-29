/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.providers.impl.packageProviders

import org.jetbrains.kotlin.analysis.providers.KotlinCompositeProvider
import org.jetbrains.kotlin.analysis.providers.KotlinPackageProvider
import org.jetbrains.kotlin.analysis.providers.impl.KotlinCompositeProviderFactory
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.TargetPlatform

public class CompositeKotlinPackageProvider private constructor(
    override val providers: List<KotlinPackageProvider>,
) : KotlinPackageProvider(), KotlinCompositeProvider<KotlinPackageProvider> {
    override fun doesPackageExist(packageFqName: FqName, platform: TargetPlatform): Boolean {
        return providers.any { it.doesPackageExist(packageFqName, platform) }
    }

    override fun doesKotlinOnlyPackageExist(packageFqName: FqName): Boolean {
        return providers.any { it.doesKotlinOnlyPackageExist(packageFqName) }
    }

    override fun doesPlatformSpecificPackageExist(packageFqName: FqName, platform: TargetPlatform): Boolean {
        return providers.any { it.doesPlatformSpecificPackageExist(packageFqName, platform) }
    }

    override fun getSubPackageFqNames(packageFqName: FqName, platform: TargetPlatform, nameFilter: (Name) -> Boolean): Set<Name> {
        return providers.flatMapTo(mutableSetOf()) { it.getSubPackageFqNames(packageFqName, platform, nameFilter) }
    }

    override fun getKotlinOnlySubPackagesFqNames(packageFqName: FqName, nameFilter: (Name) -> Boolean): Set<Name> {
        return providers.flatMapTo(mutableSetOf()) { it.getKotlinOnlySubPackagesFqNames(packageFqName, nameFilter) }
    }

    override fun getPlatformSpecificSubPackagesFqNames(
        packageFqName: FqName,
        platform: TargetPlatform,
        nameFilter: (Name) -> Boolean
    ): Set<Name> {
        return providers.flatMapTo(mutableSetOf()) { it.getPlatformSpecificSubPackagesFqNames(packageFqName, platform, nameFilter) }
    }

    public companion object {
        public val factory: KotlinCompositeProviderFactory<KotlinPackageProvider> = KotlinCompositeProviderFactory(
            EmptyKotlinPackageProvider,
            ::CompositeKotlinPackageProvider,
        )

        public fun create(providers: List<KotlinPackageProvider>): KotlinPackageProvider = factory.create(providers)
    }
}
