// !DIAGNOSTICS: -UNUSED_VALUE

fun foo(): Int {
    val i: Int
    var j = 0
    do {
        <!VAL_REASSIGNMENT!>i<!> = ++j
    } while (j < 5)
    return i
}