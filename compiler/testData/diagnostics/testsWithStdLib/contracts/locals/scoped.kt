// RUN_PIPELINE_TILL: BACKEND
// RENDER_DIAGNOSTIC_ARGUMENTS
// LANGUAGE: +ImprovedAliasTracking

@file:OptIn(ExperimentalContracts::class)

import kotlin.contracts.*

inline fun foo(block: (Int) -> Unit) {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
        scopedCalls(block)
    }
    block(2)
}

var x: Int = 1

fun bar(n: Int) {
    foo { n ->
        <!LEAKED_LOCAL("n: Int")!>x = n<!>
    }
    foo {
        <!LEAKED_LOCAL("it: Int")!>x = it<!>
    }
    foo { n ->
        println(<!LEAKED_LOCAL_THROUGH_CALL("n: Int")!>n<!>)
        <!LEAKED_LOCAL("n: Int")!>n<!>
    }
    foo {
        <!LEAKED_LOCAL("it: Int")!>it<!>
    }
    foo { n ->
        if (n > 0) <!LEAKED_LOCAL("n: Int")!>n<!> else 0
    }
}

/* GENERATED_FIR_TAGS: annotationUseSiteTargetFile, classReference, contracts, functionDeclaration, functionalType,
lambdaLiteral, localProperty, propertyDeclaration */
