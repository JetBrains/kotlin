/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.providers.impl.packageProviders

import org.jetbrains.kotlin.analysis.providers.KotlinPackageProvider
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.TargetPlatform

internal object EmptyKotlinPackageProvider : KotlinPackageProvider() {
    override fun doesPackageExist(packageFqName: FqName, platform: TargetPlatform): Boolean = false

    override fun doesKotlinOnlyPackageExist(packageFqName: FqName): Boolean = false

    override fun doesPlatformSpecificPackageExist(packageFqName: FqName, platform: TargetPlatform): Boolean = false

    override fun getSubPackageFqNames(packageFqName: FqName, platform: TargetPlatform, nameFilter: (Name) -> Boolean): Set<Name> =
        emptySet()

    override fun getKotlinOnlySubPackagesFqNames(packageFqName: FqName, nameFilter: (Name) -> Boolean): Set<Name> = emptySet()

    override fun getPlatformSpecificSubPackagesFqNames(
        packageFqName: FqName,
        platform: TargetPlatform,
        nameFilter: (Name) -> Boolean,
    ): Set<Name> = emptySet()
}
