// !DIAGNOSTICS: -UNUSED_VALUE

fun foo(f: Boolean): Int {
    val i: Int
    when (f) {
        true -> i = 1
    }
    <!VAL_REASSIGNMENT!>i<!> = 3
    return i
}