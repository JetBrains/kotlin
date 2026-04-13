// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-19665

// KT-19665: False negative USELESS_CAST with safe cast followed by safe call

class A {
    fun foo(): Int = 2
}

fun test() {
    (A().foo() <!USELESS_CAST!>as? Int<!>)?.inc() // `as?` is useless here, but no USELESS_CAST warning is reported
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, integerLiteral, nullableType, safeCall */
