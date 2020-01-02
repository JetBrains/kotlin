// !DIAGNOSTICS: -UNUSED_VALUE

fun foo(f: Boolean): Int {
    val i: Int
    if (f) {}
    else {
        i = 2
    }
    i = 3
    return i
}