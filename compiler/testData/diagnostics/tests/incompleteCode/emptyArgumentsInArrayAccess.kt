// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -UNUSED_PARAMETER
// ISSUE: KT-79116

class Get {
    operator fun get(a: Int, b: Int, c: Int) = 0
}

fun test() {
    val a = 0
    val b = 0
    val c = 0
    val getter = Get()
    <!NO_VALUE_FOR_PARAMETER!>getter[a, <!ARGUMENT_EXPECTED!><!>,]<!>
    getter[a, <!ARGUMENT_EXPECTED!><!>, <!ARGUMENT_EXPECTED!><!>,]
    getter[a, <!ARGUMENT_EXPECTED!><!>, c]
    getter[a, b, <!ARGUMENT_EXPECTED!><!>,]
    <!NO_VALUE_FOR_PARAMETER!>getter[<!ARGUMENT_EXPECTED!><!>, <!ARGUMENT_EXPECTED!><!>,]<!>
    <!NO_VALUE_FOR_PARAMETER, NO_VALUE_FOR_PARAMETER!>getter[<!ARGUMENT_EXPECTED!><!>,]<!>
    getter[<!ARGUMENT_EXPECTED!><!>, <!ARGUMENT_EXPECTED!><!>, <!ARGUMENT_EXPECTED!><!>,]
    getter[<!ARGUMENT_EXPECTED!><!>, <!ARGUMENT_EXPECTED!><!>, <!ARGUMENT_EXPECTED!><!>, <!ARGUMENT_EXPECTED, TOO_MANY_ARGUMENTS!><!>,]
    <!NO_VALUE_FOR_PARAMETER!>getter[<!ARGUMENT_EXPECTED!><!>, b,]<!>
    getter[<!ARGUMENT_EXPECTED!><!>, b, c]
    getter[<!ARGUMENT_EXPECTED!><!>, b, c,]
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, integerLiteral, localProperty, operator,
propertyDeclaration */
