/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.linkage.partial

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrRichCallableReference
import org.jetbrains.kotlin.ir.irAttribute
import org.jetbrains.kotlin.ir.linkage.partial.PartialLinkageUtils.File as PLFile

/**
 * When deserializing [IrRichCallableReference], it may happen that the originally referenced  function/property
 * ([IrRichCallableReference.reflectionTargetSymbol]) cannot be linked (such as when it was removed),
 * but the callable reference itself is still partially "functional".
 * Especially in a case of a reference to an inline function, the KCallable.invoke() call may still
 * succeed if the function body was inlined and serialized to KLib.
 * So we don't replace the whole [IrRichCallableReference] with PL error, but only curry that error in this attribute.
 * It should be thrown later on, on any operation that requires the missing reflective information (such as KCallable.name).
 *
 * Practical note: if this attribute is non-null, the [IrRichCallableReference.reflectionTargetSymbol] is also
 * non-null and bound, but its owner is only a stub declaration.
 */
var IrRichCallableReference<*>.reflectionTargetLinkageError: PartialLinkageCase? by irAttribute(copyByDefault = true)

interface PartialLinkageSupportForLowerings {
    val isEnabled: Boolean

    fun prepareLinkageError(
        doNotLog: Boolean,
        partialLinkageCase: PartialLinkageCase,
        element: IrElement,
        file: PLFile,
    ): String

    fun throwLinkageError(
        partialLinkageCase: PartialLinkageCase,
        element: IrElement,
        file: PLFile,
        doNotLog: Boolean = false
    ): IrCall

    companion object {
        val DISABLED = object : PartialLinkageSupportForLowerings {
            override val isEnabled get() = false
            override fun prepareLinkageError(
                doNotLog: Boolean,
                partialLinkageCase: PartialLinkageCase,
                element: IrElement,
                file: PLFile
            ): String = error("Should not be called")

            override fun throwLinkageError(
                partialLinkageCase: PartialLinkageCase,
                element: IrElement,
                file: PLFile,
                doNotLog: Boolean
            ): IrCall = error("Should not be called")
        }
    }
}
