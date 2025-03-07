// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-74809

fun println(a: Any?) {}

fun f() {
    val <!REDECLARATION!>`_`<!> = "one"
    val <!REDECLARATION, UNDERSCORE_IS_RESERVED!>_<!> = "two"
    println(<!UNDERSCORE_USAGE_WITHOUT_BACKTICKS!>_<!>)
    println(`_`)
}

fun g() {
    "".let { `_` -> println(`_`) }
    "".let { _ -> println(<!UNRESOLVED_REFERENCE!>_<!>) }
}

fun h() {
    val <!UNDERSCORE_IS_RESERVED!>_<!> = "three"
    println(`_`)
}
