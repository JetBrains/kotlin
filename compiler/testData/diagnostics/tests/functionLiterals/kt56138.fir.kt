// SKIP_TXT
// ISSUE: KT-56138

fun takeLambda1(f: String.() -> String) {}
fun takeLambda2(f: String.(String) -> String) {}

fun test_1() {
    val x1: String.(String) -> String = <!INITIALIZER_TYPE_MISMATCH!>{ str, <!CANNOT_INFER_PARAMETER_TYPE!>str2<!> -> "this" }<!>
    val x2: String.() -> String = { <!UNRESOLVED_REFERENCE!>it<!> }
    val x3: String.() -> String = <!INITIALIZER_TYPE_MISMATCH!>{ <!CANNOT_INFER_PARAMETER_TYPE!>x<!> -> "x" }<!>
}

fun test_2() {
    takeLambda2 <!ARGUMENT_TYPE_MISMATCH!>{ str, <!CANNOT_INFER_PARAMETER_TYPE!>str2<!> -> "this" }<!>
    takeLambda1 { <!UNRESOLVED_REFERENCE!>it<!> }
    takeLambda1 <!ARGUMENT_TYPE_MISMATCH!>{ <!CANNOT_INFER_PARAMETER_TYPE!>x<!> -> "x" }<!>
}

fun test_3(b: Boolean) {
    val x1: String.(String) -> String = if (b) {
        { str, <!CANNOT_INFER_PARAMETER_TYPE!>str2<!> -> "this" }
    } else {
        { str, <!CANNOT_INFER_PARAMETER_TYPE!>str2<!> -> "this" }
    }

    val x2: String.() -> String = if (b) {
        { <!UNRESOLVED_REFERENCE!>it<!> }
    } else {
        { <!UNRESOLVED_REFERENCE!>it<!> }
    }

    val x3: String.() -> String = if (b) {
        <!ARGUMENT_TYPE_MISMATCH!>{ <!CANNOT_INFER_PARAMETER_TYPE!>x<!> -> "x" }<!>
    } else {
        <!ARGUMENT_TYPE_MISMATCH!>{ <!CANNOT_INFER_PARAMETER_TYPE!>x<!> -> "x" }<!>
    }
}
