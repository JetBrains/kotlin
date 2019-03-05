/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve

import org.jetbrains.kotlin.analyzer.ModuleInfo
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.container.StorageComponentContainer
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.storage.StorageManager
import java.util.*

abstract class TargetPlatform(val platformName: String) {
    override fun toString() = platformName
    abstract val platform: MultiTargetPlatform
}

abstract class PlatformDependentCompilerServices {
    private data class DefaultImportsKey(val includeKotlinComparisons: Boolean, val includeLowPriorityImports: Boolean)

    private val defaultImports = LockBasedStorageManager("TargetPlatform").let { storageManager ->
        storageManager.createMemoizedFunction<DefaultImportsKey, List<ImportPath>> { (includeKotlinComparisons, includeLowPriorityImports) ->
            ArrayList<ImportPath>().apply {
                listOf(
                    "kotlin.*",
                    "kotlin.annotation.*",
                    "kotlin.collections.*",
                    "kotlin.ranges.*",
                    "kotlin.sequences.*",
                    "kotlin.text.*",
                    "kotlin.io.*"
                ).forEach { add(ImportPath.fromString(it)) }

                if (includeKotlinComparisons) {
                    add(ImportPath.fromString("kotlin.comparisons.*"))
                }

                computePlatformSpecificDefaultImports(storageManager, this)

                if (includeLowPriorityImports) {
                    addAll(defaultLowPriorityImports)
                }
            }
        }
    }

    abstract val platformConfigurator: PlatformConfigurator

    open val defaultLowPriorityImports: List<ImportPath> get() = emptyList()

    fun getDefaultImports(languageVersionSettings: LanguageVersionSettings, includeLowPriorityImports: Boolean): List<ImportPath> =
        defaultImports(
            DefaultImportsKey(
                languageVersionSettings.supportsFeature(LanguageFeature.DefaultImportOfPackageKotlinComparisons),
                includeLowPriorityImports
            )
        )

    protected abstract fun computePlatformSpecificDefaultImports(storageManager: StorageManager, result: MutableList<ImportPath>)

    open val excludedImports: List<FqName> get() = emptyList()

    // This function is used in "cat.helm.clean:0.1.1-SNAPSHOT": https://plugins.jetbrains.com/plugin/index?xmlId=cat.helm.clean
    @Suppress("DeprecatedCallableAddReplaceWith", "unused")
    @Deprecated("Use getDefaultImports(LanguageVersionSettings, Boolean) instead.", level = DeprecationLevel.ERROR)
    fun getDefaultImports(includeKotlinComparisons: Boolean): List<ImportPath> {
        return getDefaultImports(
            if (includeKotlinComparisons) LanguageVersionSettingsImpl.DEFAULT
            else LanguageVersionSettingsImpl(LanguageVersion.KOTLIN_1_0, ApiVersion.KOTLIN_1_0),
            true
        )
    }

    open fun dependencyOnBuiltIns(): ModuleInfo.DependencyOnBuiltIns = ModuleInfo.DependencyOnBuiltIns.LAST
}

interface PlatformConfigurator {
    val platformSpecificContainer: StorageComponentContainer
    fun configureModuleComponents(container: StorageComponentContainer)
    fun configureModuleDependentCheckers(container: StorageComponentContainer)
}
