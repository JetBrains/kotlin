/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.checkers.generator.diagnostics

import org.jetbrains.kotlin.fir.PrivateForInline
import org.jetbrains.kotlin.fir.checkers.generator.diagnostics.model.DiagnosticList
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtElement

@Suppress("UNUSED_VARIABLE", "LocalVariableName", "ClassName", "unused")
@OptIn(PrivateForInline::class)
object NATIVE_DIAGNOSTICS_LIST : DiagnosticList("FirNativeErrors") {
    val ALL by object : DiagnosticGroup("All") {
        val THROWS_LIST_EMPTY by error<KtElement>()
        val INCOMPATIBLE_THROWS_OVERRIDE by error<KtElement> {
            parameter<FirRegularClassSymbol>("containingClass")
        }
        val INCOMPATIBLE_THROWS_INHERITED by error<KtDeclaration> {
            parameter<Collection<FirRegularClassSymbol>>("containingClasses")
        }
        val MISSING_EXCEPTION_IN_THROWS_ON_SUSPEND by error<KtElement> {
            parameter<FqName>("exceptionName")
        }
        val INAPPLICABLE_SHARED_IMMUTABLE_PROPERTY by error<KtElement>()
        val INAPPLICABLE_SHARED_IMMUTABLE_TOP_LEVEL by error<KtElement>()
        val INAPPLICABLE_THREAD_LOCAL by error<KtElement>()
        val INAPPLICABLE_THREAD_LOCAL_TOP_LEVEL by error<KtElement>()
    }
}