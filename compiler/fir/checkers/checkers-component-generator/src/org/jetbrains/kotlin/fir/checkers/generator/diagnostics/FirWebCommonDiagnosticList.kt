/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.checkers.generator.diagnostics

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.checkers.generator.diagnostics.model.DiagnosticList
import org.jetbrains.kotlin.fir.checkers.generator.diagnostics.model.PositioningStrategy
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.util.PrivateForInline

@Suppress("ClassName", "unused")
@OptIn(PrivateForInline::class)
object WEB_COMMON_DIAGNOSTICS_LIST : DiagnosticList("FirWebCommonErrors") {
    val EXTERNALS by object : DiagnosticGroup("Externals") {
        val NESTED_EXTERNAL_DECLARATION by error<KtExpression>(PositioningStrategy.DECLARATION_SIGNATURE_OR_DEFAULT)
        val WRONG_EXTERNAL_DECLARATION by error<KtExpression>(PositioningStrategy.DECLARATION_SIGNATURE_OR_DEFAULT) {
            parameter<String>("classKind")
        }
        val NESTED_CLASS_IN_EXTERNAL_INTERFACE by error<KtExpression>(PositioningStrategy.DECLARATION_SIGNATURE_OR_DEFAULT)
        val INLINE_EXTERNAL_DECLARATION by error<KtDeclaration>(PositioningStrategy.DECLARATION_SIGNATURE_OR_DEFAULT)
        val NON_ABSTRACT_MEMBER_OF_EXTERNAL_INTERFACE by error<KtExpression>(PositioningStrategy.DECLARATION_SIGNATURE_OR_DEFAULT)
        val EXTERNAL_CLASS_CONSTRUCTOR_PROPERTY_PARAMETER by error<KtParameter>()
        val EXTERNAL_ANONYMOUS_INITIALIZER by error<KtAnonymousInitializer>()
        val EXTERNAL_DELEGATION by error<KtElement>()
        val EXTERNAL_DELEGATED_CONSTRUCTOR_CALL by error<KtElement>()
        val WRONG_BODY_OF_EXTERNAL_DECLARATION by error<KtElement>()
        val WRONG_INITIALIZER_OF_EXTERNAL_DECLARATION by error<KtElement>()
        val WRONG_DEFAULT_VALUE_FOR_EXTERNAL_FUN_PARAMETER by error<KtElement>()
    }
}
