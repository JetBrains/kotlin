/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.tests.compilation.model

import java.nio.file.Path

/**
 * Represents a cache key used within the scenario DSL to provide initial build cacheability.
 * Must properly override the [equals] and [hashCode] methods.
 */
interface DependencyScenarioDslCacheKey

interface Dependency {
    /**
     * File system location that may be used as a part of JVM classpath.
     */
    val location: Path
    val scenarioDslCacheKey: DependencyScenarioDslCacheKey
}