/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import org.gradle.api.invocation.Gradle
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import java.io.File
import java.util.concurrent.Callable
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

/**
 * [BuildService] that looks up properties in the following precedence order:
 *   1. Root project's `local.properties` file
 *   2. Project's Gradle properties ([org.gradle.api.provider.ProviderFactory.gradleProperty])
 */
internal abstract class BuildPropertiesBuildService @Inject constructor(
    private val providerFactory: ProviderFactory
) : BuildService<BuildPropertiesBuildService.Params> {

    interface Params : BuildServiceParameters {
        val localProperties: MapProperty<String, String>
    }

    private val propertiesPerProject = ConcurrentHashMap<String, Provider<String>>()

    private val localProperties by lazy { parameters.localProperties.get() }

    internal fun property(
        propertyName: String
    ): Provider<String> {
        return propertiesPerProject.computeIfAbsent("gradleProp/$propertyName") {
            val valueFromGradleAndLocalProperties = MemoizedCallable {
                localProperties[propertyName]
                    ?: providerFactory.gradleProperty(propertyName).orNull
            }
            providerFactory.provider { valueFromGradleAndLocalProperties.call() }
        }
    }

    internal fun systemProperty(
        propertyName: String
    ): Provider<String> {
        return propertiesPerProject.computeIfAbsent("systemProp/$propertyName") {
            val valueFromSystemProperty = MemoizedCallable {
                providerFactory.systemProperty(propertyName).orNull
            }
            providerFactory.provider { valueFromSystemProperty.call() }
        }
    }

    internal fun envProperty(
        propertyName: String
    ): Provider<String> {
        return propertiesPerProject.computeIfAbsent("envProp/$propertyName") {
            val valueFromEnvProperty = MemoizedCallable {
                providerFactory.environmentVariable(propertyName).orNull
            }
            providerFactory.provider { valueFromEnvProperty.call() }
        }
    }

    companion object {

        fun registerIfAbsent(
            gradle: Gradle,
            providerFactory: ProviderFactory,
            rootDir: File,
        ): Provider<BuildPropertiesBuildService> =
            gradle.sharedServices.registerIfAbsent(
                BuildPropertiesBuildService::class.qualifiedName!!,
                BuildPropertiesBuildService::class.java
            ) {
                it.parameters.localProperties.set(
                    providerFactory.localProperties(rootDir)
                )
            }
    }

    private class MemoizedCallable<T>(valueResolver: Callable<T>) : Callable<T> {
        private val value: T? by lazy { valueResolver.call() }
        override fun call(): T? = value
    }
}

