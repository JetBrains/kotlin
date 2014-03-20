// !DIAGNOSTICS: -UNUSED_PARAMETER
        
fun Int.invoke(i: Int, a: Any) {}
fun Int.invoke(a: Any, i: Int) {}

fun foo(i: Int) {
    <!OVERLOAD_RESOLUTION_AMBIGUITY!>i<!>(1, 1)

    <!OVERLOAD_RESOLUTION_AMBIGUITY!>5<!>(1, 2)
}
