/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.common

import org.jetbrains.kotlin.descriptors.DescriptorVisibility
import org.jetbrains.kotlin.descriptors.PlatformDescriptorVisibilityProvider
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.descriptors.java.JavaVisibilities
import org.jetbrains.kotlin.load.java.JavaDescriptorVisibilities

/**
 * Resolves the JVM platform-specific visibilities (package-private, protected-static, protected-and-package) that the
 * platform-agnostic [org.jetbrains.kotlin.descriptors.DescriptorVisibilities] does not know about.
 */
class JavaPlatformDescriptorVisibilityProvider : PlatformDescriptorVisibilityProvider {
    override fun toDescriptorVisibility(visibility: Visibility): DescriptorVisibility? = when (visibility) {
        JavaVisibilities.PackageVisibility -> JavaDescriptorVisibilities.PACKAGE_VISIBILITY
        JavaVisibilities.ProtectedStaticVisibility -> JavaDescriptorVisibilities.PROTECTED_STATIC_VISIBILITY
        JavaVisibilities.ProtectedAndPackage -> JavaDescriptorVisibilities.PROTECTED_AND_PACKAGE
        else -> null
    }
}
