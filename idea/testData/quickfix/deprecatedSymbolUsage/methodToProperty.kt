// "Replace with 'x'" "true"

trait X {
    @deprecated("", ReplaceWith("x"))
    fun getX(): String

    val x: String
}

fun foo(x: X): String {
    return x.<caret>getX()
}
