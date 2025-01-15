/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.serialization

import org.jetbrains.kotlin.ir.declarations.IrExternalPackageFragment
import org.jetbrains.kotlin.ir.declarations.IrFile

class InlineFunctionFilesTracker() {
    private val inlineFunctionFiles = mutableMapOf<IrExternalPackageFragment, IrFile>()

    fun add(packageFragment: IrExternalPackageFragment, file: IrFile) {
        inlineFunctionFiles[packageFragment]?.let {
            require(it == file) {
                "Different files ${it.fileEntry.name} and ${file.fileEntry.name} have the same $packageFragment"
            }
        }
        inlineFunctionFiles[packageFragment] = file
    }

    fun getOrNull(packageFragment: IrExternalPackageFragment): IrFile? = inlineFunctionFiles[packageFragment]
}