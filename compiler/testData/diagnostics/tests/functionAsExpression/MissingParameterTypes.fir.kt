// !CHECK_TYPE
// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_ANONYMOUS_PARAMETER -UNUSED_VARIABLE

val a = fun (<!VALUE_PARAMETER_WITH_NO_TYPE_ANNOTATION!>x<!>) = x

val b: (Int) -> Int = fun (x) = x + 3

val c: (Int, String) -> Int = <!INITIALIZER_TYPE_MISMATCH!>fun (x) = 3<!>

val d: (Int, String) -> Int = <!INITIALIZER_TYPE_MISMATCH!>fun (x) = 3<!>

val e: (Int, String) -> Int = <!INITIALIZER_TYPE_MISMATCH!>fun (x: String) = 3<!>

val f: (Int) -> Int = <!INITIALIZER_TYPE_MISMATCH!>fun (x: String) = 3<!>

fun test1(a: (Int) -> Unit) {
    test1(fun (x) { checkSubtype<Int>(x)})
}

fun test2(a: (Int) -> Unit) {
    test2(<!ARGUMENT_TYPE_MISMATCH!>fun (x: String) {}<!>)
}

fun test3(a: (Int, String) -> Unit) {
    test3(<!ARGUMENT_TYPE_MISMATCH!>fun (x: String) {}<!>)
}
