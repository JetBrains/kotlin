/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve

import org.jetbrains.kotlin.fir.resolve.providers.impl.FirFallbackBuiltinSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.impl.getTopLevelClassifierNamesInPackage
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.resolve.DefaultImportProvider
import org.jetbrains.kotlin.resolve.ImportPath
import org.jetbrains.kotlin.storage.StorageManager

object FirJvmDefaultImportProvider : DefaultImportProvider() {
    override fun computePlatformSpecificDefaultImports(storageManager: StorageManager, result: MutableList<ImportPath>) {
        result.add(ImportPath.fromString("kotlin.jvm.*"))

        for (builtInsPackage in StandardClassIds.builtInsPackagesWithDefaultNamedImport) {
            getTopLevelClassifierNamesInPackage(FirFallbackBuiltinSymbolProvider.builtInsPackageFragments, builtInsPackage).forEach {
                result.add(ImportPath(builtInsPackage.child(it), false))
            }
        }
    }

    override val defaultLowPriorityImports: List<ImportPath> =
        listOf(ImportPath.fromString("java.lang.*"))
}