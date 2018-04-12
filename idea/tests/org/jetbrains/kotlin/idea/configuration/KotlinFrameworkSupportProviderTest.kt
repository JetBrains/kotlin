/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.configuration

import com.intellij.framework.addSupport.FrameworkSupportInModuleProvider
import com.intellij.ide.util.frameworkSupport.FrameworkSupportProviderTestCase
import junit.framework.TestCase
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.config.VersionView
import org.jetbrains.kotlin.config.apiVersionView
import org.jetbrains.kotlin.config.languageVersionView
import org.jetbrains.kotlin.idea.compiler.configuration.KotlinCommonCompilerArgumentsHolder
import org.jetbrains.kotlin.idea.framework.JSFrameworkSupportProvider
import org.jetbrains.kotlin.idea.framework.JavaFrameworkSupportProvider

class KotlinFrameworkSupportProviderTest : FrameworkSupportProviderTestCase() {
    private fun doTest(provider: FrameworkSupportInModuleProvider) {
        selectFramework(provider).createLibraryDescription()
        addSupport()

        with (KotlinCommonCompilerArgumentsHolder.getInstance(module.project).settings) {
            TestCase.assertEquals(VersionView.Specific(LanguageVersion.LATEST_STABLE), languageVersionView)
            TestCase.assertEquals(VersionView.Specific(LanguageVersion.LATEST_STABLE), apiVersionView)
        }
    }

    fun testJvm() = doTest(JavaFrameworkSupportProvider())

    fun testJs() = doTest(JSFrameworkSupportProvider())
}