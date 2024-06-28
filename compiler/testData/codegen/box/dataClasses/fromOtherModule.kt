// MODULE: lib
// FILE: lib.kt

data class D(val x: Int)

// MODULE: main(lib)
// FILE: main.kt

fun box() : String {
    val a = D(1)
    val b = D(2)
    val c = D(1)

    if (a == b) return "FAIL 1"
    if (a != c) return "FAIL 2"
    if (!a.equals(c)) return "FAIL 3"
    if (a.hashCode() != c.hashCode()) return "FAIL 4"
    return "OK"
}