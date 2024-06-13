// FIR_IDENTICAL
// DIAGNOSTICS: -UNREACHABLE_CODE

typealias MyNothing = Nothing

fun foo(n: Nothing, n2: MyNothing) {
    val a: Unit = when(n) {}
    val b: Unit = when(n2) {}
}