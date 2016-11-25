external interface I

fun box(a: Any, b: Any): Pair<I, I?> {
    return Pair(<!UNCHECKED_CAST_TO_NATIVE_INTERFACE!>a as I<!>, <!UNCHECKED_CAST_TO_NATIVE_INTERFACE!>b as? I<!>)
}