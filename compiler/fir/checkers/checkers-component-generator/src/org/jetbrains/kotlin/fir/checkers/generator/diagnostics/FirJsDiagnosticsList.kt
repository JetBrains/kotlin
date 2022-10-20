/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.checkers.generator.diagnostics

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.PrivateForInline
import org.jetbrains.kotlin.fir.checkers.generator.diagnostics.model.DiagnosticList
import org.jetbrains.kotlin.fir.checkers.generator.diagnostics.model.PositioningStrategy
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.psi.*

@Suppress("UNUSED_VARIABLE", "LocalVariableName", "ClassName", "unused")
@OptIn(PrivateForInline::class)
object JS_DIAGNOSTICS_LIST : DiagnosticList("FirJsErrors") {
    val ANNOTATIONS by object : DiagnosticGroup("Annotations") {
        val WRONG_JS_QUALIFIER by error<KtExpression>()
        val JS_MODULE_PROHIBITED_ON_VAR by error<KtAnnotationEntry>()
        val RUNTIME_ANNOTATION_NOT_SUPPORTED by warning<PsiElement>(PositioningStrategy.DECLARATION_SIGNATURE_OR_DEFAULT)
        val RUNTIME_ANNOTATION_ON_EXTERNAL_DECLARATION by error<PsiElement>(PositioningStrategy.DECLARATION_SIGNATURE_OR_DEFAULT)
    }

    val SUPERTYPES by object : DiagnosticGroup("Supertypes") {
        val WRONG_MULTIPLE_INHERITANCE by error<KtElement>(PositioningStrategy.DECLARATION_SIGNATURE_OR_DEFAULT) {
            parameter<FirCallableSymbol<*>>("symbol")
        }
    }

    val FUN_INTERFACES by object : DiagnosticGroup("Fun Interfaces") {
        val IMPLEMENTING_FUNCTION_INTERFACE by error<KtClassOrObject>(PositioningStrategy.DECLARATION_SIGNATURE_OR_DEFAULT)
    }

    val EXTERNAL by object : DiagnosticGroup("External") {
        val OVERRIDING_EXTERNAL_FUN_WITH_OPTIONAL_PARAMS by error<KtElement>(PositioningStrategy.DECLARATION_SIGNATURE_OR_DEFAULT)
        val OVERRIDING_EXTERNAL_FUN_WITH_OPTIONAL_PARAMS_WITH_FAKE by error<KtElement>(PositioningStrategy.DECLARATION_SIGNATURE_OR_DEFAULT) {
            parameter<FirNamedFunctionSymbol>("function")
        }
        val CALL_TO_DEFINED_EXTERNALLY_FROM_NON_EXTERNAL_DECLARATION by error<PsiElement>()
        val EXTERNAL_CLASS_CONSTRUCTOR_PROPERTY_PARAMETER by error<KtParameter>()
        val EXTERNAL_ENUM_ENTRY_WITH_BODY by error<KtElement>()
        val EXTERNAL_ANONYMOUS_INITIALIZER by error<KtAnonymousInitializer>()
        val EXTERNAL_DELEGATION by error<KtElement>()
        val EXTERNAL_DELEGATED_CONSTRUCTOR_CALL by error<KtElement>()
        val WRONG_BODY_OF_EXTERNAL_DECLARATION by error<KtElement>()
        val WRONG_INITIALIZER_OF_EXTERNAL_DECLARATION by error<KtElement>()
        val WRONG_DEFAULT_VALUE_FOR_EXTERNAL_FUN_PARAMETER by error<KtElement>()
        val NESTED_EXTERNAL_DECLARATION by error<KtExpression>(PositioningStrategy.DECLARATION_SIGNATURE_OR_DEFAULT)
        val WRONG_EXTERNAL_DECLARATION by error<KtExpression>(PositioningStrategy.DECLARATION_SIGNATURE_OR_DEFAULT) {
            parameter<String>("class kind")
        }
        val NESTED_CLASS_IN_EXTERNAL_INTERFACE by error<KtExpression>(PositioningStrategy.DECLARATION_SIGNATURE_OR_DEFAULT)
        val EXTERNAL_TYPE_EXTENDS_NON_EXTERNAL_TYPE by error<KtElement>(PositioningStrategy.DECLARATION_SIGNATURE_OR_DEFAULT)
        val INLINE_EXTERNAL_DECLARATION by error<KtDeclaration>(PositioningStrategy.DECLARATION_SIGNATURE_OR_DEFAULT)
        val INLINE_CLASS_IN_EXTERNAL_DECLARATION_WARNING by warning<KtElement>(PositioningStrategy.DECLARATION_SIGNATURE_OR_DEFAULT)
        val INLINE_CLASS_IN_EXTERNAL_DECLARATION by error<KtElement>(PositioningStrategy.DECLARATION_SIGNATURE_OR_DEFAULT)
        val EXTENSION_FUNCTION_IN_EXTERNAL_DECLARATION by error<KtElement>(PositioningStrategy.DECLARATION_SIGNATURE_OR_DEFAULT)
        val NON_ABSTRACT_MEMBER_OF_EXTERNAL_INTERFACE by error<KtExpression>(PositioningStrategy.DECLARATION_SIGNATURE_OR_DEFAULT)
    }

    val DYNAMICS by object : DiagnosticGroup("Dynamics") {
        val DELEGATION_BY_DYNAMIC by error<KtElement>()
    }

    val DEBUG_INFO by object : DiagnosticGroup("Debug Info") {
        val DYNAMIC by debugInfo<KtElement>()
    }
}
