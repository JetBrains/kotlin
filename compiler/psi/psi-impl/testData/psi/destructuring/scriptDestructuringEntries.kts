// COMPILATION_ERRORS
// Script-level destructuring entries become script properties, so they are stubbed
annotation class Ann

data class P(val first: Int, val second: String)

val p = P(1, "")

var (@Ann a: Int, _, `c`) = p

val [d, @Ann e: String] = p

(val f = first, var g: String = second) = p

[var h, val i] = p

val (`_`, a_b) = p

(val m first) = p

(val n =) = p

class C {
    val (j, k) = p
}
