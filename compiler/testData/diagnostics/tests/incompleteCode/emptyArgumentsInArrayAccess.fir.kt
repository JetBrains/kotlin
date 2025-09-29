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
    <!NO_VALUE_FOR_PARAMETER!>getter[a, <!EMPTY_ARGUMENT_IN_ARRAY_ACCESS!><!>,]<!>
    getter[a, <!EMPTY_ARGUMENT_IN_ARRAY_ACCESS!><!>, <!EMPTY_ARGUMENT_IN_ARRAY_ACCESS!><!>,]
    getter[a, <!EMPTY_ARGUMENT_IN_ARRAY_ACCESS!><!>, c]
    getter[a, b, <!EMPTY_ARGUMENT_IN_ARRAY_ACCESS!><!>,]
    <!NO_VALUE_FOR_PARAMETER!>getter[<!EMPTY_ARGUMENT_IN_ARRAY_ACCESS!><!>, <!EMPTY_ARGUMENT_IN_ARRAY_ACCESS!><!>,]<!>
    <!NO_VALUE_FOR_PARAMETER, NO_VALUE_FOR_PARAMETER!>getter[<!EMPTY_ARGUMENT_IN_ARRAY_ACCESS!><!>,]<!>
    getter[<!EMPTY_ARGUMENT_IN_ARRAY_ACCESS!><!>, <!EMPTY_ARGUMENT_IN_ARRAY_ACCESS!><!>, <!EMPTY_ARGUMENT_IN_ARRAY_ACCESS!><!>,]
    getter[<!EMPTY_ARGUMENT_IN_ARRAY_ACCESS!><!>, <!EMPTY_ARGUMENT_IN_ARRAY_ACCESS!><!>, <!EMPTY_ARGUMENT_IN_ARRAY_ACCESS!><!>, <!EMPTY_ARGUMENT_IN_ARRAY_ACCESS, TOO_MANY_ARGUMENTS!><!>,]
    <!NO_VALUE_FOR_PARAMETER!>getter[<!EMPTY_ARGUMENT_IN_ARRAY_ACCESS!><!>, b,]<!>
    getter[<!EMPTY_ARGUMENT_IN_ARRAY_ACCESS!><!>, b, c]
    getter[<!EMPTY_ARGUMENT_IN_ARRAY_ACCESS!><!>, b, c,]
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, integerLiteral, localProperty, operator,
propertyDeclaration */
