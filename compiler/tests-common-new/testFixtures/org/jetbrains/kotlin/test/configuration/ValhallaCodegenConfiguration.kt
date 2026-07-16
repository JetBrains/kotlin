/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.configuration

import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.config.ValhallaSupportMode
import org.jetbrains.kotlin.test.TestJdkKind
import org.jetbrains.kotlin.test.builders.RegisteredDirectivesBuilder
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.IGNORE_DEXING
import org.jetbrains.kotlin.test.directives.ConfigurationDirectives.WITH_STDLIB
import org.jetbrains.kotlin.test.directives.JvmEnvironmentConfigurationDirectives.JDK_KIND
import org.jetbrains.kotlin.test.directives.JvmEnvironmentConfigurationDirectives.JVM_TARGET
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives.ENABLE_JVM_PREVIEW
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives.VALHALLA_SUPPORT
import org.jetbrains.kotlin.test.services.MetaTestConfigurator
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.util.KtTestUtil

/**
 * The JVM target the Valhalla codegen tests compile against. Preview features (`-Xjvm-enable-preview`) are only valid on the exact
 * JDK feature version that produced them, so this must match the JDK provided via `JDK_VALHALLA` (currently a Project Valhalla build
 * based on JDK 27).
 */
internal val VALHALLA_JVM_TARGET = JvmTarget.JVM_27

/**
 * The default directives of the Project Valhalla codegen tests: they enable Valhalla support (`-Xvalhalla-support=primitives`) and
 * JVM preview features (`-Xjvm-enable-preview`), compile against [VALHALLA_JVM_TARGET] and run on the Valhalla JDK
 * ([TestJdkKind.FULL_JDK_VALHALLA], taken from the `JDK_VALHALLA` property), and disable the dexing check ([IGNORE_DEXING]) since D8
 * does not support the class file version of a recent JVM target.
 */
internal fun RegisteredDirectivesBuilder.configureValhallaDefaultDirectives() {
    JDK_KIND with TestJdkKind.FULL_JDK_VALHALLA
    JVM_TARGET with VALHALLA_JVM_TARGET
    VALHALLA_SUPPORT with ValhallaSupportMode.PRIMITIVES
    +ENABLE_JVM_PREVIEW
    +IGNORE_DEXING
    +WITH_STDLIB
}

/**
 * Skips the test unless the Valhalla JDK is provided via the `JDK_VALHALLA` property. Used by test runners that are shared with
 * non-Valhalla tests (where the skip can't be applied by overriding the runner itself).
 */
internal class ValhallaJdkAvailabilitySkipper(testServices: TestServices) : MetaTestConfigurator(testServices) {
    override fun shouldSkipTest(): Boolean = !KtTestUtil.isJdkValhallaAvailable()
}
