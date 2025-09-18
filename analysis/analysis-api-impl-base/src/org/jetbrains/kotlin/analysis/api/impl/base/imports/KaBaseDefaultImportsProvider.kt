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
import org.jetbrains.kotlin.resolve.DefaultImportsProvider
import org.jetbrains.kotlin.resolve.ImportPath
import java.util.concurrent.ConcurrentHashMap

@KaImplementationDetail
abstract class KaBaseDefaultImportsProvider : KaDefaultImportsProvider {
    private val cache =
        ConcurrentHashMap<DefaultImportsProvider, KaDefaultImports>(6/* JVM, JS, NATIVE, WASM(JS/WASI), and COMMON*/, 1.0f)

    protected abstract fun getCompilerDefaultImportsProvider(targetPlatform: TargetPlatform): DefaultImportsProvider

    override fun getDefaultImports(
        targetPlatform: TargetPlatform,
    ): KaDefaultImports {
        val firDefaultImportsProvider = getCompilerDefaultImportsProvider(targetPlatform)
        return cache.getOrPut(firDefaultImportsProvider) { createDefaultImports(firDefaultImportsProvider) }
    }

    private fun createDefaultImports(firDefaultImportsProvider: DefaultImportsProvider): KaDefaultImportsImpl = KaDefaultImportsImpl(
        defaultImports = getKaDefaultImports(firDefaultImportsProvider),
        excludedFromDefaultImports = firDefaultImportsProvider.excludedImports.map { ImportPath(it, isAllUnder = false) }
    )

    private fun getKaDefaultImports(firDefaultImportsProvider: DefaultImportsProvider): List<KaDefaultImport> = buildList {
        firDefaultImportsProvider.getDefaultImports(
            includeLowPriorityImports = false
        ).mapTo(this) { import ->
            KaDefaultImportImpl(ImportPath(import.fqName, import.isAllUnder), KaDefaultImportPriority.HIGH)
        }
        firDefaultImportsProvider.defaultLowPriorityImports.mapTo(this) { import ->
            KaDefaultImportImpl(ImportPath(import.fqName, import.isAllUnder), KaDefaultImportPriority.LOW)
        }
    }
}
