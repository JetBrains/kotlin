/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.resolve.jvm.diagnostics

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOriginKind.OTHER

enum class MemberKind { FIELD, METHOD }

data class RawSignature(val name: String, val desc: String, val kind: MemberKind)

open class JvmDeclarationOrigin(
    val originKind: JvmDeclarationOriginKind,
    val element: PsiElement?,
    val descriptor: DeclarationDescriptor?,
) {
    // This property is used to get the original element in the sources, from which this declaration was generated.
    // In the old JVM backend, it is just the PSI element. In JVM IR, it is the original IR element (before any deep copy).
    open val originalSourceElement: Any?
        get() = element

    override fun toString(): String =
        if (this == NO_ORIGIN) "NO_ORIGIN" else "origin=$originKind element=${element?.javaClass?.simpleName} descriptor=$descriptor"

    companion object {
        @JvmField
        val NO_ORIGIN: JvmDeclarationOrigin = JvmDeclarationOrigin(OTHER, null, null)
    }
}
