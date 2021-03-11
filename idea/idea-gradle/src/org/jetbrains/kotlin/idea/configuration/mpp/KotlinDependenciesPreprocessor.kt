/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.configuration.mpp

import org.jetbrains.kotlin.gradle.KotlinDependency

internal interface KotlinDependenciesPreprocessor {
    operator fun invoke(dependencies: Iterable<KotlinDependency>): List<KotlinDependency>
}

internal operator fun KotlinDependenciesPreprocessor.plus(
    other: KotlinDependenciesPreprocessor
): KotlinDependenciesPreprocessor {
    return SequentialKotlinDependenciesPreprocessor(this, other)
}

private class SequentialKotlinDependenciesPreprocessor(
    private val first: KotlinDependenciesPreprocessor,
    private val second: KotlinDependenciesPreprocessor
) : KotlinDependenciesPreprocessor {
    override fun invoke(dependencies: Iterable<KotlinDependency>): List<KotlinDependency> {
        return second(first(dependencies))
    }
}