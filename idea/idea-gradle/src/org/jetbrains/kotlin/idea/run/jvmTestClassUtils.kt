/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.run

import com.intellij.execution.Location
import com.intellij.execution.actions.ConfigurationFromContext
import com.intellij.execution.junit.JUnitConfigurationProducer
import com.intellij.execution.testframework.AbstractPatternBasedConfigurationProducer
import com.intellij.ide.plugins.PluginManager
import com.intellij.openapi.extensions.PluginId.getId
import com.intellij.openapi.util.component1
import com.intellij.openapi.util.component2
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMethod

private val isJUnitEnabled by lazy { isPluginEnabled("JUnit") }
private val isTestNgEnabled by lazy { isPluginEnabled("TestNG-J") }

private fun isPluginEnabled(id: String): Boolean {
    return PluginManager.isPluginInstalled(getId(id)) && !PluginManager.isDisabled(id)
}

internal fun ConfigurationFromContext.isJpsJunitConfiguration(): Boolean {
    return isProducedBy(JUnitConfigurationProducer::class.java)
            || isProducedBy(AbstractPatternBasedConfigurationProducer::class.java)
}

internal fun canRunJvmTests() = isJUnitEnabled || isTestNgEnabled

internal fun getTestClassForJvm(location: Location<*>): PsiClass? {
    val leaf = location.psiElement ?: return null

    if (isJUnitEnabled) {
        KotlinJUnitRunConfigurationProducer.getTestClass(leaf)?.let { return it }
    }
    if (isTestNgEnabled) {
        KotlinTestNgConfigurationProducer.getTestClassAndMethod(leaf)?.let { (testClass, testMethod) ->
            return if (testMethod == null) testClass else null
        }
    }
    return null
}

internal fun getTestMethodForJvm(location: Location<*>): PsiMethod? {
    val leaf = location.psiElement ?: return null

    if (isJUnitEnabled) {
        KotlinJUnitRunConfigurationProducer.getTestMethod(leaf)?.let { return it }
    }
    if (isTestNgEnabled) {
        KotlinTestNgConfigurationProducer.getTestClassAndMethod(leaf)?.second?.let { return it }
    }
    return null
}