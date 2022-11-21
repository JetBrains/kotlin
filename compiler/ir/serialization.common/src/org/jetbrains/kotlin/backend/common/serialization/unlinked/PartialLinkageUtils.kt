/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization.unlinked

import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrAnonymousInitializer
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrComposite
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull

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

fun IrClass.isPartiallyLinked(): Boolean = origin is PartiallyLinkedDeclarationOrigin
        || declarations.firstIsInstanceOrNull<IrAnonymousInitializer>()?.body?.statements?.any(IrStatement::isPartialLinkageRuntimeError) == true

fun IrStatement.isPartialLinkageRuntimeError(): Boolean {
    return PartiallyLinkedStatementOrigin.PARTIAL_LINKAGE_RUNTIME_ERROR == when (this) {
        is IrCall -> origin
        is IrComposite -> origin
        else -> return false
    }
}
