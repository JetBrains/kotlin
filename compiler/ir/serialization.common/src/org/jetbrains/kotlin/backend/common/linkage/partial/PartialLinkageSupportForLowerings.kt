/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.linkage.partial

import org.jetbrains.kotlin.backend.common.linkage.partial.ClassifierExplorer.Companion.classifierLinkageStatusCache
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrRichCallableReference
import org.jetbrains.kotlin.ir.irAttribute
import org.jetbrains.kotlin.backend.common.linkage.partial.PartialLinkageSources.File as PLFile

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

/**
 * Returns the partial linkage status of this class, indicating whether it is considered usable after the partial linkage phase or not.
 * The result reflects whether the class or any of its dependencies (such as supertypes, type parameters, or annotations) are missing or invalid
 */
val IrClass.partialLinkageStatus: ClassifierPartialLinkageStatus?
    get() = classifierLinkageStatusCache

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
