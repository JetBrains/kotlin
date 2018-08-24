/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jvm.compiler.tmp.impls

import org.jetbrains.kotlin.cli.jvm.compiler.tmp.*
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.psiUtil.startOffset

// Equality expected to work correctly only with elements from the same file
abstract class AbstractRDumpElement(override val offset: Int, override val presentableText: String) : RDumpElement {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AbstractRDumpElement

        if (offset != other.offset) return false

        return true
    }

    override fun hashCode(): Int = offset

    override fun toString(): String = presentableText
}

abstract class AbstractRDumpExpression(offset: Int, presentableText: String, override val type: RDumpType) :
    AbstractRDumpElement(offset, presentableText), RDumpExpression

class RDumpResolvedCall(
    offset: Int,
    presentableText: String,
    type: RDumpType,
    val candidateDescriptor: RDumpDescriptor,
    val typeArguments: List<RDumpType>
) : AbstractRDumpExpression(offset, presentableText, type) {

    constructor(
        original: KtElement,
        type: RDumpType,
        candidateDescriptor: RDumpDescriptor,
        typeArguments: List<RDumpType>
    ) : this(original.startOffset, original.text, type, candidateDescriptor, typeArguments)
}

class RDumpErrorCall(offset: Int, presentableText: String, val diagnostics: List<RDumpDiagnostic>) :
    AbstractRDumpExpression(offset, presentableText, RDumpErrorType) {

    constructor(
        original: KtElement,
        vararg diagnostics: RDumpDiagnostic
    ) : this(original.startOffset, original.text, diagnostics.toList())
}
