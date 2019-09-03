/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve

import org.jetbrains.kotlin.analyzer.ModuleInfo
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.storage.StorageManager
import java.util.ArrayList

abstract class PlatformDependentAnalyzerServices {
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

    abstract fun computePlatformSpecificDefaultImports(storageManager: StorageManager, result: MutableList<ImportPath>)

    open val excludedImports: List<FqName> get() = emptyList()

    open fun dependencyOnBuiltIns(): ModuleInfo.DependencyOnBuiltIns =
        ModuleInfo.DependencyOnBuiltIns.LAST
}