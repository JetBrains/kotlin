/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.project.structure

import org.jetbrains.kotlin.analysis.providers.KotlinPackageProvider
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.TargetPlatform

class CompositeKotlinPackageProvider
private constructor(
    private val providers: List<KotlinPackageProvider>
) : KotlinPackageProvider() {

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

    companion object {
        fun create(providers: List<KotlinPackageProvider>): KotlinPackageProvider {
            return when (providers.size) {
                0 -> EmptyKotlinPackageProvider
                1 -> providers.single()
                else -> CompositeKotlinPackageProvider(providers)
            }
        }
    }
}
