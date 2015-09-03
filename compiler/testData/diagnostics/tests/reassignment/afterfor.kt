// !DIAGNOSTICS: -UNUSED_VALUE

fun foo(k: Int): Int {
    val i: Int
    for (j in 1..k) {
        <!VAL_REASSIGNMENT!>i<!> = j
    }
    i = 6
    return i
}