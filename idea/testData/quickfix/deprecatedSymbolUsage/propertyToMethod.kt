// "Replace with 'getX()'" "true"

trait X {
    @Deprecated("", ReplaceWith("getX()"))
    val x: String

    fun getX(): String
}

fun foo(x: X): String {
    return x.<caret>x
}
