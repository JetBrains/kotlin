// !WITH_NEW_INFERENCE
// !CHECK_TYPE
// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_ANONYMOUS_PARAMETER -UNUSED_VARIABLE

val a = fun (x) = x

val b: (Int) -> Int = fun (x) = x + 3

val c: (Int, String) -> Int = fun (x) = 3

val d: (Int, String) -> Int = fun (x) = 3

val e: (Int, String) -> Int = fun (x: String) = 3

val f: (Int) -> Int = fun (x: String) = 3

fun test1(a: (Int) -> Unit) {
    test1(fun (x) { checkSubtype<Int>(x)})
}

fun test2(a: (Int) -> Unit) {
    <!INAPPLICABLE_CANDIDATE!>test2<!>(fun (x: String) {})
}

fun test3(a: (Int, String) -> Unit) {
    <!INAPPLICABLE_CANDIDATE!>test3<!>(fun (x: String) {})
}
