/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors

/**
 * Maps a platform-specific [Visibility] (e.g. the JVM package-private or protected-static visibilities) to its
 * corresponding [DescriptorVisibility].
 *
 * [DescriptorVisibilities.toDescriptorVisibility] only knows the platform-agnostic visibilities; platform ones live in
 * modules it cannot depend on (e.g. `JavaDescriptorVisibilities` in `core/descriptors.jvm`). This provider is discovered
 * via [java.util.ServiceLoader] so the platform-agnostic core can resolve them without a compile-time dependency, and
 * without relying on the platform visibility classes being class-loaded first.
 *
 * Mirrors the existing [org.jetbrains.kotlin.util.ModuleVisibilityHelper] service in the same class.
 */
interface PlatformDescriptorVisibilityProvider {
    /**
     * Returns the [DescriptorVisibility] corresponding to [visibility], or `null` if [visibility] is not a
     * platform-specific one handled by this provider (in which case the caller keeps its own resolution).
     */
    fun toDescriptorVisibility(visibility: Visibility): DescriptorVisibility?

    object Default : PlatformDescriptorVisibilityProvider {
        override fun toDescriptorVisibility(visibility: Visibility): DescriptorVisibility? = null
    }
}
