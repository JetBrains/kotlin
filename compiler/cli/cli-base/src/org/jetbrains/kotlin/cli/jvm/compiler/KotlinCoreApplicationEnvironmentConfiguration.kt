/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jvm.compiler

/**
 * A configuration for [KotlinCoreApplicationEnvironment], which may contain any number of [KotlinCoreApplicationEnvironmentConfigurator]s
 * to configure the application environment and application.
 *
 * An application environment configuration must have a clear identity. It is recommended to define the configuration as a unique object.
 * This is because [KotlinCoreEnvironment.getOrCreateApplicationEnvironment] tries to share application environments to different
 * requesters, but it can only keep a single application environment for a single application environment configuration at the same time.
 * The fewer distinct configurations there are, the easier it is to share application environments.
 *
 * Given that application environments are shared, the configuration must not rely on being invoked once per test. It is instead invoked
 * once per application environment creation, which may occur only once in an entire test suite (if all tests share the same application
 * environment configuration).
 */
interface KotlinCoreApplicationEnvironmentConfiguration {
    /**
     * [isUnitTestMode] configures [KotlinCoreApplicationEnvironment.myUnitTestMode].
     */
    val isUnitTestMode: Boolean

    val configurators: List<KotlinCoreApplicationEnvironmentConfigurator>
}

/**
 * A configurator for an aspect of a [KotlinCoreApplicationEnvironment]. It may, for example, register services with the environment's
 * application, or register additional file types.
 */
interface KotlinCoreApplicationEnvironmentConfigurator {
    fun configure(applicationEnvironment: KotlinCoreApplicationEnvironment)
}

/**
 * The default application environment configurations which should be used when no additional configuration of the application environment
 * is needed.
 *
 * Please prefer using these configurations instead of building a custom "empty" configuration to facilitate application sharing.
 */
object DefaultKotlinCoreApplicationEnvironmentConfigurations {
    val PRODUCTION = object : KotlinCoreApplicationEnvironmentConfiguration {
        override val isUnitTestMode: Boolean get() = false
        override val configurators: List<KotlinCoreApplicationEnvironmentConfigurator> get() = emptyList()
    }

    val TEST = object : KotlinCoreApplicationEnvironmentConfiguration {
        override val isUnitTestMode: Boolean get() = true
        override val configurators: List<KotlinCoreApplicationEnvironmentConfigurator> get() = emptyList()
    }

    fun getByUnitTestMode(isUnitTestMode: Boolean): KotlinCoreApplicationEnvironmentConfiguration = if (isUnitTestMode) TEST else PRODUCTION
}

abstract class KotlinCoreApplicationEnvironmentConfigurationBase : KotlinCoreApplicationEnvironmentConfiguration {
    private val _configurators = mutableListOf<KotlinCoreApplicationEnvironmentConfigurator>()

    override val configurators: List<KotlinCoreApplicationEnvironmentConfigurator> = _configurators

    protected fun addConfigurator(configurator: KotlinCoreApplicationEnvironmentConfigurator) {
        _configurators.add(configurator)
    }
}
