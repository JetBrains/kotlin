/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.descriptors

import org.jetbrains.kotlin.builtins.BuiltInsPackageFragment
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.name.FqName

class FirBuiltInsPackageFragment(
    fqName: FqName,
    moduleDescriptor: ModuleDescriptor
) : FirPackageFragmentDescriptor(fqName, moduleDescriptor), BuiltInsPackageFragment {
    override val isFallback: Boolean
        get() = false
}