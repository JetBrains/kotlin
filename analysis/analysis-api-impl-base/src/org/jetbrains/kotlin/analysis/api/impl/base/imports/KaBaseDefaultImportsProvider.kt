/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.imports

import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.imports.KaDefaultImport
import org.jetbrains.kotlin.analysis.api.imports.KaDefaultImportPriority
import org.jetbrains.kotlin.analysis.api.imports.KaDefaultImports
import org.jetbrains.kotlin.analysis.api.imports.KaDefaultImportsProvider
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.resolve.DefaultImportProvider
import org.jetbrains.kotlin.resolve.ImportPath
import java.util.concurrent.ConcurrentHashMap

@KaImplementationDetail
abstract class KaBaseDefaultImportsProvider : KaDefaultImportsProvider {
    private val cache =
        ConcurrentHashMap<DefaultImportProvider, KaDefaultImports>(6/* JVM, JS, NATIVE, WASM(JS/WASI), and COMMON*/, 1.0f)

    protected abstract fun getCompilerDefaultImportProvider(targetPlatform: TargetPlatform): DefaultImportProvider

    override fun getDefaultImports(
        targetPlatform: TargetPlatform,
    ): KaDefaultImports {
        val firDefaultImportProvider = getCompilerDefaultImportProvider(targetPlatform)
        return cache.getOrPut(firDefaultImportProvider) { createDefaultImports(firDefaultImportProvider) }
    }

    private fun createDefaultImports(firDefaultImportProvider: DefaultImportProvider): KaDefaultImportsImpl = KaDefaultImportsImpl(
        defaultImports = getKaDefaultImports(firDefaultImportProvider),
        excludedFromDefaultImports = firDefaultImportProvider.excludedImports.map { ImportPath(it, isAllUnder = false) }
    )

    private fun getKaDefaultImports(firDefaultImportProvider: DefaultImportProvider): List<KaDefaultImport> = buildList {
        firDefaultImportProvider.getDefaultImports(
            defaultImportOfPackageKotlinComparisons = true /*supported since Kotlin 1.1*/,
            includeLowPriorityImports = false
        ).mapTo(this) { import ->
            KaDefaultImportImpl(ImportPath(import.fqName, import.isAllUnder), KaDefaultImportPriority.HIGH)
        }
        firDefaultImportProvider.defaultLowPriorityImports.mapTo(this) { import ->
            KaDefaultImportImpl(ImportPath(import.fqName, import.isAllUnder), KaDefaultImportPriority.LOW)
        }
    }
}