/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ide.konan

import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.actions.LazyRunConfigurationProducer
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.module.Module
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiElement
import com.intellij.psi.util.parentOfType
import org.jetbrains.kotlin.idea.facet.externalSystemNativeMainRunTasks
import org.jetbrains.kotlin.idea.project.platform
import org.jetbrains.kotlin.idea.util.module
import org.jetbrains.kotlin.platform.konan.isNative
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.plugins.gradle.service.execution.GradleExternalTaskConfigurationType
import org.jetbrains.plugins.gradle.service.execution.GradleRunConfiguration


class KotlinNativeRunConfigurationProducer :
    LazyRunConfigurationProducer<GradleRunConfiguration>(),
    KotlinNativeRunConfigurationProvider {

    override val isForTests = false

    override fun getConfigurationFactory(): ConfigurationFactory =
        GradleExternalTaskConfigurationType.getInstance().factory

    override fun isConfigurationFromContext(configuration: GradleRunConfiguration, context: ConfigurationContext): Boolean {
        val module = context.module.asNativeModule() ?: return false
        val location = context.location ?: return false
        val function = location.psiElement.parentOfType<KtFunction>() ?: return false

        val mainRunTasks = getDebugMainRunTasks(function)
        if (mainRunTasks.isEmpty()) return false

        return configuration.settings.externalProjectPath == ExternalSystemApiUtil.getExternalProjectPath(module)
                && configuration.settings.taskNames == mainRunTasks
    }

    override fun setupConfigurationFromContext(
        configuration: GradleRunConfiguration,
        context: ConfigurationContext,
        sourceElement: Ref<PsiElement>
    ): Boolean {
        val module = context.module.asNativeModule() ?: return false
        val function = sourceElement.get()?.parentOfType<KtFunction>() ?: return false

        val mainRunTasks = getDebugMainRunTasks(function)
        if (mainRunTasks.isEmpty()) return false

        configuration.settings.apply {
            externalProjectPath = ExternalSystemApiUtil.getExternalProjectPath(module)
            taskNames = mainRunTasks
        }
        configuration.name = mainRunTasks.first()
        configuration.isScriptDebugEnabled = false

        return true
    }

    private fun Module?.asNativeModule(): Module? = takeIf { it?.platform.isNative() }

    private fun getDebugMainRunTasks(function: KtFunction): List<String> {
        val functionName = function.fqName?.asString() ?: return emptyList()
        val module = function.module ?: return emptyList()

        return module.externalSystemNativeMainRunTasks()
            .filter { it.debuggable && it.entryPoint == functionName }
            .map { it.taskName }
    }
}