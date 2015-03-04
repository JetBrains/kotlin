// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_VARIABLE

val a = fun (<!CANNOT_INFER_PARAMETER_TYPE!>x<!>) = <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>x<!>

val b: (Int) -> Int = fun (x) = x + 3

val c: (Int, String) -> Int = <!TYPE_MISMATCH!>fun <!EXPECTED_PARAMETERS_NUMBER_MISMATCH!>(x)<!> = 3<!>

val d: (Int, String) -> Int = <!TYPE_MISMATCH!>fun <!EXPECTED_PARAMETERS_NUMBER_MISMATCH!>(x)<!> = 3<!>

val e: (Int, String) -> Int = <!TYPE_MISMATCH!>fun <!EXPECTED_PARAMETERS_NUMBER_MISMATCH!>(<!EXPECTED_PARAMETER_TYPE_MISMATCH!>x: String<!>)<!> = 3<!>

val f: (Int) -> Int = <!TYPE_MISMATCH!>fun (<!EXPECTED_PARAMETER_TYPE_MISMATCH!>x: String<!>) = 3<!>

fun test1(a: (Int) -> Unit) {
    test1(fun (<!CANNOT_INFER_PARAMETER_TYPE!>x<!>) { <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>x<!> : Int})
}

fun test2(a: (Int) -> Unit) {
    test2(<!TYPE_MISMATCH!>fun (x: String) {}<!>)
}

fun test3(a: (Int, String) -> Unit) {
    test3(<!TYPE_MISMATCH!>fun (x: String) {}<!>)
}