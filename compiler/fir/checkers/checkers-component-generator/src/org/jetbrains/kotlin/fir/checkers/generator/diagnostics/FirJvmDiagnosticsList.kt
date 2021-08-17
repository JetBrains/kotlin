/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.checkers.generator.diagnostics

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.PrivateForInline
import org.jetbrains.kotlin.fir.checkers.generator.diagnostics.model.DiagnosticList
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.fir.checkers.generator.diagnostics.model.*
import org.jetbrains.kotlin.psi.KtAnnotationEntry

@Suppress("UNUSED_VARIABLE", "LocalVariableName", "ClassName", "unused")
@OptIn(PrivateForInline::class)
object JVM_DIAGNOSTICS_LIST : DiagnosticList("FirJvmErrors") {
    val DECLARATIONS by object : DiagnosticGroup("Declarations") {
        val CONFLICTING_JVM_DECLARATIONS by error<PsiElement>()
        val STRICTFP_ON_CLASS by error<KtAnnotationEntry>()
        val VOLATILE_ON_VALUE by error<KtAnnotationEntry>()
        val VOLATILE_ON_DELEGATE by error<KtAnnotationEntry>()
    }

    val TYPES by object : DiagnosticGroup("Types") {
        val JAVA_TYPE_MISMATCH by error<KtExpression> {
            parameter<ConeKotlinType>("expectedType")
            parameter<ConeKotlinType>("actualType")
        }
    }
}
