/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.bir.backend.builders

import org.jetbrains.kotlin.bir.SourceSpan
import org.jetbrains.kotlin.bir.backend.BirBackendContext
import org.jetbrains.kotlin.bir.backend.utils.listOfNulls
import org.jetbrains.kotlin.bir.declarations.*
import org.jetbrains.kotlin.bir.declarations.impl.BirVariableImpl
import org.jetbrains.kotlin.bir.expressions.*
import org.jetbrains.kotlin.bir.expressions.impl.*
import org.jetbrains.kotlin.bir.resetWithNulls
import org.jetbrains.kotlin.bir.symbols.*
import org.jetbrains.kotlin.bir.types.BirType
import org.jetbrains.kotlin.bir.types.utils.defaultType
import org.jetbrains.kotlin.bir.types.utils.typeWith
import org.jetbrains.kotlin.bir.util.constructedClass
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.IrTypeOperator
import org.jetbrains.kotlin.name.Name
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract


context(BirBackendContext)
@OptIn(ExperimentalContracts::class)
inline fun <R> birBodyScope(block: context(BirStatementBuilderScope) () -> R): R {
    contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
    return block(BirStatementBuilderScope())
}

context(BirBackendContext)
class BirStatementBuilderScope() {
    var sourceSpan: SourceSpan = SourceSpan.UNDEFINED
    var origin: IrStatementOrigin? = null
    var returnTarget: BirReturnTargetSymbol? = null

    private var lastTemporaryIndex: Int = 0
    fun nextTemporaryIndex(): Int = lastTemporaryIndex++

    fun inventIndexedNameForTemporary(prefix: String = "tmp", nameHint: String? = null): String {
        val index = nextTemporaryIndex()
        return if (nameHint != null) "$prefix${index}_$nameHint" else "$prefix$index"
    }

    fun getNameForTemporary(nameHint: String?, addIndexToName: Boolean): String =
        if (addIndexToName) inventIndexedNameForTemporary("tmp", nameHint)
        else nameHint ?: "tmp"
}