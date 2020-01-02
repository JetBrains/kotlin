// !DIAGNOSTICS: -UNUSED_VALUE

fun foo(f: Boolean): Int {
    val i: Int
    when (f) {
        true -> i = 1
    }
    i = 3
    return i
}