// !DIAGNOSTICS: -UNUSED_PARAMETER

var count = 0

operator fun Int.get(s: Int): Int {
    count++
    return this + s
}

operator fun Int.set(s: Int, x: String = "", z: Int) {
}

fun main() {
    1[2] = 1
    1.set(2, z = 1)
    1[2] += 1

    1.set(2, <!NO_VALUE_FOR_PARAMETER!><!ARGUMENT_TYPE_MISMATCH!>1<!>)<!>
}
