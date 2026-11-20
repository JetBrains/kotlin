// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB
// LANGUAGE: +EagerLambdaAnalysis
// ISSUE: KT-90660

@file:OptIn(ExperimentalContracts::class)

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

open class Scope<X> {
    @JvmName("fooString")
    fun foo(x: X, block: () -> String) {
        contract {
            callsInPlace(block, InvocationKind.EXACTLY_ONCE)
        }

        block()
    }

    fun foo(x: X, block: () -> Int) {
        contract {
            callsInPlace(block, InvocationKind.EXACTLY_ONCE)
        }

        block()
    }

    fun unsubstituted(x: X) {
        <!OVERLOAD_RESOLUTION_AMBIGUITY!>foo<!>(x) { "" }
    }
}

class Substituted : Scope<Int>() {
    fun substitutedViaImplicitReceiver() {
        <!OVERLOAD_RESOLUTION_AMBIGUITY!>foo<!>(1) { "" }
    }
}

fun substitutedViaExplicitReceiver(scope: Scope<Int>) {
    scope.<!OVERLOAD_RESOLUTION_AMBIGUITY!>foo<!>(1) { "" }
}

/* GENERATED_FIR_TAGS: annotationUseSiteTargetFile, classDeclaration, classReference, contractCallsEffect, contracts,
functionDeclaration, functionalType, integerLiteral, lambdaLiteral, nullableType, stringLiteral, typeParameter */
