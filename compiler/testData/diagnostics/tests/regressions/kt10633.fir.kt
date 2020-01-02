// !DIAGNOSTICS: -UNUSED_PARAMETER

var count = 0

operator fun Int.get(s: Int): Int {
    count++
    return this + s
}

operator fun Int.set(s: Int, x: String = "", z: Int) {
}

fun main() {
    <!INAPPLICABLE_CANDIDATE!>1[2] = 1<!>
    1.set(2, z = 1)
    1[2] += 1

    1.<!INAPPLICABLE_CANDIDATE!>set<!>(2, 1)
}
