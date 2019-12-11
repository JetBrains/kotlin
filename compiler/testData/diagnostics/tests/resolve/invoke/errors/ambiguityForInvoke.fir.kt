// !WITH_NEW_INFERENCE
// !DIAGNOSTICS: -UNUSED_PARAMETER
        
fun Int.invoke(i: Int, a: Any) {}
fun Int.invoke(a: Any, i: Int) {}

fun foo(i: Int) {
    <!AMBIGUITY!>i<!>(1, 1)

    <!AMBIGUITY!>5(1, 2)<!>
}
