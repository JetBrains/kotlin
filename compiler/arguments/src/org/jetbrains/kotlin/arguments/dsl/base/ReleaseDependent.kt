/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.arguments.dsl.base

/**
 * A type which value could change between releases.
 */
class ReleaseDependent<T>(
    val current: T,
    val valueInVersions: Map<ClosedRange<KotlinReleaseVersion>, T>
)

internal fun <T> ReleaseDependent(
    current: T,
    vararg oldValues: Pair<ClosedRange<KotlinReleaseVersion>, T>,
) = ReleaseDependent(current, oldValues.associate { it })

internal fun <T> T.asReleaseDependent() = ReleaseDependent(this)
