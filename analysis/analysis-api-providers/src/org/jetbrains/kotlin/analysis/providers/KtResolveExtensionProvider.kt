/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.providers

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.util.ModificationTracker
import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.name.FqName

/**
 * Allows to extend Kotlin resolution by generating addintional declarations.
 *
 * Those declarations will be analyzed the same way as they were just regular source files inside the project
 */
abstract class KtResolveExtensionProvider {
    abstract fun provideExtensionsFor(module: KtModule): List<KtResolveExtension>

    companion object {
        val EP_NAME = ExtensionPointName<KtResolveExtensionProvider>("org.jetbrains.kotlin.ktResolveExtensionProvider")

        fun provideExtensionsFor(module: KtModule): List<KtResolveExtension> {
            return EP_NAME.getExtensionList(module.project).flatMap { it.provideExtensionsFor(module) }
        }
    }
}

abstract class KtResolveExtension {
    /**
     * Get the list of files which should be generated for the module.
     *
     * Those files should remain valid until the tracker [getModificationTracker] is not modified.
     *
     * Returned files should contain valid Kotlin code.
     * All declaration types should be specified explicitly and no declaration bodies should be present.
     */
    abstract fun getKtFiles(): List<KtResolveExtensionFile>

    /**
     * @return the [ModificationTracker] which controls the validity lifecycle of the declarations
     */
    abstract fun getModificationTracker(): ModificationTracker
    abstract fun getPackagesToBeResolved(): Set<FqName>?
}

data class KtResolveExtensionFile(
    val text: String,
    val fileName: String,
) {
    init {
        require(fileName.endsWith(".kt")) {
            "Only Kotlin files can be generated"
        }
    }
}