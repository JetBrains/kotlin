/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors

import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

interface PackageFragmentProviderWithSecondaryFragments : PackageFragmentProvider {
    fun getSecondaryPackageFragments(fqName: FqName): List<PackageFragmentDescriptor>

    fun getSecondarySubPackagesOf(fqName: FqName, nameFilter: (Name) -> Boolean): Collection<FqName>
}