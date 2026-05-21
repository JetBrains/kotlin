/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import org.gradle.api.Plugin

/**
 * Just a marker plugin to make classes like [KotlinBuildProperties] available on the buildscript's classpath
 */
class KotlinBuildPlugin : Plugin<Any> {
    override fun apply(target: Any) {
        // no-op
    }
}
