// FIR_IDENTICAL
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
    getter[a,<!SYNTAX!><!> ,]
    getter[a,<!SYNTAX!><!> ,<!SYNTAX!><!> ,]
    <!NO_VALUE_FOR_PARAMETER!>getter[a,<!SYNTAX!><!> , c]<!>
    getter[a, b,<!SYNTAX!><!> ,]
    getter[<!SYNTAX!><!>,<!SYNTAX!><!> ,]
    getter[<!SYNTAX!><!>,]
    getter[<!SYNTAX!><!>,<!SYNTAX!><!> ,<!SYNTAX!><!> ,]
    getter[<!SYNTAX!><!>,<!SYNTAX!><!> ,<!SYNTAX!><!> ,<!SYNTAX!><!> ,]
    <!NO_VALUE_FOR_PARAMETER, NO_VALUE_FOR_PARAMETER!>getter[<!SYNTAX!><!>, b,]<!>
    <!NO_VALUE_FOR_PARAMETER!>getter[<!SYNTAX!><!>, b, c]<!>
    <!NO_VALUE_FOR_PARAMETER!>getter[<!SYNTAX!><!>, b, c,]<!>
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, integerLiteral, localProperty, operator,
propertyDeclaration */
