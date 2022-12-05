/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization.unlinked

import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.name.Name

internal object PartialLinkageUtils {
    val UNKNOWN_NAME = Name.identifier("<unknown name>")

    fun IdSignature.guessName(nameSegmentsToPickUp: Int): String? = when (this) {
        is IdSignature.CommonSignature -> if (nameSegmentsToPickUp == 1)
            shortName
        else
            nameSegments.takeLast(nameSegmentsToPickUp).joinToString(".")

        is IdSignature.CompositeSignature -> inner.guessName(nameSegmentsToPickUp)
        is IdSignature.AccessorSignature -> accessorSignature.guessName(nameSegmentsToPickUp)

        else -> null
    }
}

/** An optimization to avoid re-computing file for every visited declaration */
internal abstract class FileAwareIrElementTransformerVoid(startingFile: ABIVisibility.File?) : IrElementTransformerVoid() {
    private var _currentFile: ABIVisibility.File? = startingFile
    protected val currentFile: ABIVisibility.File get() = _currentFile ?: error("No information about current file")

    override fun visitFile(declaration: IrFile): IrFile {
        _currentFile = ABIVisibility.File.IrBased(declaration)
        return try {
            super.visitFile(declaration)
        } finally {
            _currentFile = null
        }
    }
}
