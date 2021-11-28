// FULL_JDK
// WITH_STDLIB

class C(val xs: MutableList<String>)

fun box(): String {
    val c = C(ArrayList<String>())
    c.xs += listOf("OK")
    return c.xs[0]
}