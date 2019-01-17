import org.gradle.api.plugins.ExtensionAware
import org.gradle.kotlin.dsl.configure

/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

fun org.gradle.plugins.ide.idea.model.IdeaProject.settings(block: org.jetbrains.gradle.ext.ProjectSettings.() -> Unit) =
    (this@settings as ExtensionAware).extensions.configure(block)

fun org.jetbrains.gradle.ext.ProjectSettings.compiler(block: org.jetbrains.gradle.ext.IdeaCompilerConfiguration.() -> Unit) =
    (this@compiler as ExtensionAware).extensions.configure(block)

fun org.jetbrains.gradle.ext.ProjectSettings.delegateActions(block: org.jetbrains.gradle.ext.ActionDelegationConfig.() -> Unit) =
    (this@delegateActions as ExtensionAware).extensions.configure(block)

fun org.jetbrains.gradle.ext.ProjectSettings.runConfigurations(block: org.jetbrains.gradle.ext.DefaultRunConfigurationContainer.() -> Unit) =
    (this@runConfigurations as ExtensionAware).extensions.configure(block)

inline fun <reified T: org.jetbrains.gradle.ext.RunConfiguration> org.jetbrains.gradle.ext.DefaultRunConfigurationContainer.defaults(noinline block: T.() -> Unit) =
    defaults(T::class.java, block)

fun org.jetbrains.gradle.ext.DefaultRunConfigurationContainer.junit(name: String, block: org.jetbrains.gradle.ext.JUnit.() -> Unit) =
    create(name, org.jetbrains.gradle.ext.JUnit::class.java, block)

fun org.jetbrains.gradle.ext.DefaultRunConfigurationContainer.application(name: String, block: org.jetbrains.gradle.ext.Application.() -> Unit) =
    create(name, org.jetbrains.gradle.ext.Application::class.java, block)

