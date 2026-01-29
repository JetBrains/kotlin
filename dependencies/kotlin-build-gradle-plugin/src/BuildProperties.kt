/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:Suppress("unused", "MemberVisibilityCanBePrivate")

import org.gradle.api.Project
import org.gradle.api.initialization.Settings
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory

class KotlinBuildProperties internal constructor(
    private val propertiesBuildService: Provider<BuildPropertiesBuildService>,
    private val providerFactory: ProviderFactory,
) {

    fun stringProperty(name: String): Provider<String> = propertiesBuildService.flatMap {
        it.property(name)
    }

    fun booleanProperty(
        name: String,
        defaultValue: Boolean = false,
    ): Provider<Boolean> = booleanProperty(name, providerFactory.provider { defaultValue })

    fun booleanProperty(
        name: String,
        defaultValue: Provider<Boolean>,
    ): Provider<Boolean> = propertiesBuildService.flatMap {
        it.property(name).toBoolean(defaultValue)
    }

    fun intProperty(name: String): Provider<Int> = propertiesBuildService.flatMap {
        it.property(name).map { it.trim().toIntOrNull() }
    }

    val isInIdeaSync: Provider<Boolean> = propertiesBuildService.flatMap {
        it.systemProperty("idea.sync.active").toBoolean()
    }

    val isTeamcityBuild: Provider<Boolean> = booleanProperty(
        "teamcity",
        propertiesBuildService.flatMap { it.envProperty("TEAMCITY_VERSION").map { true }.orElse(false) }
    )

    /**
     * Nullable
     */
    val buildCacheUrl: Provider<String> = stringProperty("kotlin.build.cache.url")

    val pushToBuildCache: Provider<Boolean> = booleanProperty("kotlin.build.cache.push", false)

    val localBuildCacheEnabled: Provider<Boolean> = booleanProperty("kotlin.build.cache.local.enabled", isTeamcityBuild.map { !it })

    /**
     * Nullable
     */
    val localBuildCacheDirectory: Provider<String> = stringProperty("kotlin.build.cache.local.directory")

    /**
     * Nullable
     */
    val buildScanServer: Provider<String> = stringProperty("kotlin.build.scan.url")

    /**
     * Nullable
     */
    val buildCacheUser: Provider<String> = stringProperty("kotlin.build.cache.user")

    /**
     * Nullable
     */
    val buildCachePassword: Provider<String> = stringProperty("kotlin.build.cache.password")

    /**
     * Nullable
     */
    val buildGradlePluginVersion: Provider<String> = stringProperty("kotlin.build.gradlePlugin.version")

    /**
     * Nullable
     */
    val kotlinBootstrapVersion: Provider<String> = stringProperty("bootstrap.kotlin.default.version")

    /**
     * Nullable
     */
    val defaultSnapshotVersion: Provider<String> = stringProperty("defaultSnapshotVersion")

    /**
     * Nullable
     */
    val customBootstrapVersion: Provider<String> = stringProperty("bootstrap.kotlin.version")

    /**
     * Nullable
     */
    val customBootstrapRepo: Provider<String> = stringProperty("bootstrap.kotlin.repo")

    val localBootstrap: Provider<Boolean> = booleanProperty("bootstrap.local")

    /**
     * Nullable
     */
    val localBootstrapVersion: Provider<String> = stringProperty("bootstrap.local.version")

    /**
     * Nullable
     */
    val localBootstrapPath: Provider<String> = stringProperty("bootstrap.local.path")

    val useFir: Provider<Boolean> = booleanProperty("kotlin.build.useFir")

    val useFirIdeaPlugin: Provider<Boolean> = booleanProperty("idea.fir.plugin")

    /**
     * Nullable
     */
    val teamCityBootstrapVersion: Provider<String> = stringProperty("bootstrap.teamcity.kotlin.version")

    /**
     * Nullable
     */
    val teamCityBootstrapBuildNumber: Provider<String> = stringProperty("bootstrap.teamcity.build.number")

    /**
     * Nullable
     */
    val teamCityBootstrapProject: Provider<String> = stringProperty("bootstrap.teamcity.project")

    /**
     * Nullable
     */
    val teamCityBootstrapUrl: Provider<String> = stringProperty("bootstrap.teamcity.url")

    val isKotlinNativeEnabled: Provider<Boolean> = booleanProperty("kotlin.native.enabled")

    val renderDiagnosticNames: Provider<Boolean> = booleanProperty("kotlin.build.render.diagnostic.names")

    val isCacheRedirectorEnabled: Provider<Boolean> = booleanProperty("cacheRedirectorEnabled")

    private fun Provider<String>.toBoolean(defaultValue: Boolean = false): Provider<Boolean> = map {
        if (it.isEmpty()) return@map true // has property without value means 'true'
        return@map it.trim().toBoolean()
    }.orElse(defaultValue)

    private fun Provider<String>.toBoolean(defaultValue: Provider<Boolean>): Provider<Boolean> = map {
        if (it.isEmpty()) return@map true // has property without value means 'true'
        return@map it.trim().toBoolean()
    }.orElse(defaultValue)
}

val Project.kotlinBuildProperties: KotlinBuildProperties
    get() = KotlinBuildProperties(
        BuildPropertiesBuildService.registerIfAbsent(
            gradle,
            providers,
            rootDir,
        ),
        providers,
    )

fun getKotlinBuildPropertiesForSettings(settings: Any) = (settings as Settings).kotlinBuildProperties

val Settings.kotlinBuildProperties: KotlinBuildProperties
    get() = KotlinBuildProperties(
        BuildPropertiesBuildService.registerIfAbsent(
            gradle,
            providers,
            rootDir,
        ),
        providers,
    )
