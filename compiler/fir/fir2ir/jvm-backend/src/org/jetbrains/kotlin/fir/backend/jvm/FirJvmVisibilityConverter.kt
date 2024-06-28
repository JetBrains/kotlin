/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend.jvm

import org.jetbrains.kotlin.descriptors.java.JavaVisibilities
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fir.backend.Fir2IrVisibilityConverter
import org.jetbrains.kotlin.descriptors.DescriptorVisibility


object FirJvmVisibilityConverter : Fir2IrVisibilityConverter() {
    override fun convertPlatformVisibility(visibility: Visibility): DescriptorVisibility {
        return when (visibility) {
            JavaVisibilities.PackageVisibility -> org.jetbrains.kotlin.load.java.JavaDescriptorVisibilities.PACKAGE_VISIBILITY
            JavaVisibilities.ProtectedStaticVisibility -> org.jetbrains.kotlin.load.java.JavaDescriptorVisibilities.PROTECTED_STATIC_VISIBILITY
            JavaVisibilities.ProtectedAndPackage -> org.jetbrains.kotlin.load.java.JavaDescriptorVisibilities.PROTECTED_AND_PACKAGE
            else -> error("Unknown visibility: $visibility")
        }
    }
}
