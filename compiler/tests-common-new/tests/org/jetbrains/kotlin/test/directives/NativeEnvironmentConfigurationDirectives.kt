/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.directives

import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer

object NativeEnvironmentConfigurationDirectives : SimpleDirectivesContainer() {
    /** See also [org.jetbrains.kotlin.test.services.configuration.NativeEnvironmentConfigurator.getRuntimePathsForModule] */
    val WITH_PLATFORM_LIBS by directive(
        """
            Add all available Kotlin/Native platform libs for the Kotlin/Native target in effect to the classpath.
            The Kotlin/Native target is determined by the NativeEnvironmentConfigurator class.
        """.trimIndent()
    )

    /** See also [WITH_PLATFORM_LIBS], [org.jetbrains.kotlin.test.services.configuration.NativeEnvironmentConfigurator.getNativeTarget]. */
    val WITH_FIXED_TARGET by stringDirective("Run tests with the fixed Kotlin/Native target.")
}
