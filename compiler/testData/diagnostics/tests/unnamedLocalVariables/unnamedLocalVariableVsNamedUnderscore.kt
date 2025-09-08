// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-74809
// LANGUAGE: +UnnamedLocalVariables

fun println(a: Any?) {}

fun f() {
    val `_` = "one"
    val <!UNDERSCORE_IS_RESERVED!>_<!> = "two"
    println(<!UNDERSCORE_USAGE_WITHOUT_BACKTICKS!>_<!>)
    println(`_`)
}

fun g() {
    "".let { `_` -> println(`_`) }
    "".let { _ -> println(<!UNRESOLVED_REFERENCE!>_<!>) }
}

fun h() {
    val <!UNDERSCORE_IS_RESERVED!>_<!> = "three"
    println(<!UNRESOLVED_REFERENCE!>`_`<!>)
}

/* GENERATED_FIR_TAGS: functionDeclaration, lambdaLiteral, localProperty, nullableType, propertyDeclaration,
stringLiteral, unnamedLocalVariable */
