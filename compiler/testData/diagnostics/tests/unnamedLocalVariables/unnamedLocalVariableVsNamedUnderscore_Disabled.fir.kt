// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-74809
// LANGUAGE: -UnnamedLocalVariables

fun println(a: Any?) {}

fun f() {
    val `_` = "one"
    val <!UNSUPPORTED_FEATURE!>_<!> = "two"
    println(<!UNDERSCORE_USAGE_WITHOUT_BACKTICKS!>_<!>)
    println(`_`)
}

fun g() {
    "".let { `_` -> println(`_`) }
    "".let { _ -> println(<!UNRESOLVED_REFERENCE!>_<!>) }
}

fun h() {
    val <!UNSUPPORTED_FEATURE!>_<!> = "three"
    println(<!UNRESOLVED_REFERENCE!>`_`<!>)
}
