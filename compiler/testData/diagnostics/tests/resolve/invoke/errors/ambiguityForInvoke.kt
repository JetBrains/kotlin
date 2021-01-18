// !WITH_NEW_INFERENCE
// !DIAGNOSTICS: -UNUSED_PARAMETER
        
fun Int.invoke(i: Int, a: Any) {}
fun Int.invoke(a: Any, i: Int) {}

fun foo(i: Int) {
    <!NONE_APPLICABLE{NI}, OVERLOAD_RESOLUTION_AMBIGUITY{OI}!>i<!>(1, 1)

    <!NONE_APPLICABLE{NI}, OVERLOAD_RESOLUTION_AMBIGUITY{OI}!>5<!>(1, 2)
}
