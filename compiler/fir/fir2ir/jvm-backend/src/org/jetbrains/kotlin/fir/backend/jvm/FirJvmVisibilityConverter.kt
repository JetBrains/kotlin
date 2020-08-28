/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend.jvm

import org.jetbrains.kotlin.fir.JavaVisibilities
import org.jetbrains.kotlin.fir.Visibility
import org.jetbrains.kotlin.fir.backend.Fir2IrVisibilityConverter
import org.jetbrains.kotlin.descriptors.Visibility as OldVisibility
import org.jetbrains.kotlin.load.java.JavaVisibilities as OldJavaVisibilities


object FirJvmVisibilityConverter : Fir2IrVisibilityConverter() {
    override fun convertPlatformVisibility(visibility: Visibility): OldVisibility {
        return when (visibility) {
            JavaVisibilities.PackageVisibility -> OldJavaVisibilities.PACKAGE_VISIBILITY
            JavaVisibilities.ProtectedStaticVisibility -> OldJavaVisibilities.PROTECTED_STATIC_VISIBILITY
            JavaVisibilities.ProtectedAndPackage -> OldJavaVisibilities.PROTECTED_AND_PACKAGE
            else -> error("Unknown visibility: $this")
        }
    }
}
