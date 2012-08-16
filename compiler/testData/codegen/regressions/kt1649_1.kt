trait A {
    val method : (() -> Unit)?
}

fun test(a : A) {
    if (a.method != null) {
        a.method!!()
    }
}