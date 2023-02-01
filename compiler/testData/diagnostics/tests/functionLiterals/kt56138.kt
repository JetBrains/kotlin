// SKIP_TXT
// ISSUE: KT-56138

fun takeLambda1(f: String.() -> String) {}
fun takeLambda2(f: String.(String) -> String) {}

fun test_1() {
    val x1: String.(String) -> String = { <!EXPECTED_PARAMETERS_NUMBER_MISMATCH!>str, <!CANNOT_INFER_PARAMETER_TYPE!>str2<!><!> -> "this" }
    val x2: String.() -> String = { <!UNRESOLVED_REFERENCE!>it<!> }
    val x3: String.() -> String = { <!CANNOT_INFER_PARAMETER_TYPE, EXPECTED_PARAMETERS_NUMBER_MISMATCH!>x<!> -> "x" }
}

fun test_2() {
    takeLambda2 <!TYPE_MISMATCH!>{ <!EXPECTED_PARAMETERS_NUMBER_MISMATCH!>str, <!CANNOT_INFER_PARAMETER_TYPE!>str2<!><!> -> "this" }<!>
    takeLambda1 <!TYPE_MISMATCH!>{ <!UNRESOLVED_REFERENCE!>it<!> }<!>
    takeLambda1 <!TYPE_MISMATCH!>{ <!CANNOT_INFER_PARAMETER_TYPE, EXPECTED_PARAMETERS_NUMBER_MISMATCH!>x<!> -> "x" }<!>
}

fun test_3(b: Boolean) {
    val x1: String.(String) -> String = if (b) <!TYPE_MISMATCH!>{
        { <!EXPECTED_PARAMETERS_NUMBER_MISMATCH!>str, <!CANNOT_INFER_PARAMETER_TYPE!>str2<!><!> -> "this" }
    }<!> else <!TYPE_MISMATCH!>{
        { <!EXPECTED_PARAMETERS_NUMBER_MISMATCH!>str, <!CANNOT_INFER_PARAMETER_TYPE!>str2<!><!> -> "this" }
    }<!>

    val x2: String.() -> String = if (b) <!TYPE_MISMATCH!>{
        { <!UNRESOLVED_REFERENCE!>it<!> }
    }<!> else <!TYPE_MISMATCH!>{
        { <!UNRESOLVED_REFERENCE!>it<!> }
    }<!>

    val x3: String.() -> String = if (b) <!TYPE_MISMATCH!>{
        { <!CANNOT_INFER_PARAMETER_TYPE, EXPECTED_PARAMETERS_NUMBER_MISMATCH!>x<!> -> "x" }
    }<!> else <!TYPE_MISMATCH!>{
        { <!CANNOT_INFER_PARAMETER_TYPE, EXPECTED_PARAMETERS_NUMBER_MISMATCH!>x<!> -> "x" }
    }<!>
}
