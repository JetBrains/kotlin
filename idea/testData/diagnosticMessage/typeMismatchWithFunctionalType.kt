// !DIAGNOSTICS_NUMBER: 1
// !DIAGNOSTICS: TYPE_MISMATCH
// !MESSAGE_TYPE: TEXT

fun foo(handler: (s: String) -> Unit) {
    bar(handler)
}

fun bar(a: (n: Int) -> Unit) {}
