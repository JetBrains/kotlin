/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.platform.packages

import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.TargetPlatform

public object KotlinEmptyPackageProvider : KotlinPackageProvider {
    override fun doesPackageExist(packageFqName: FqName, platform: TargetPlatform): Boolean = false

    override fun doesKotlinOnlyPackageExist(packageFqName: FqName): Boolean = false

    override fun doesPlatformSpecificPackageExist(packageFqName: FqName, platform: TargetPlatform): Boolean = false

    override fun getSubpackageNames(packageFqName: FqName, platform: TargetPlatform): Set<Name> =
        emptySet()

    override fun getKotlinOnlySubpackageNames(packageFqName: FqName): Set<Name> = emptySet()

    override fun getPlatformSpecificSubpackageNames(
        packageFqName: FqName,
        platform: TargetPlatform,
    ): Set<Name> = emptySet()
}
