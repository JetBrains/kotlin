// "Replace with 'c.newFun(this)'" "true"

class X {
    @Deprecated("", ReplaceWith("c.newFun(this)"))
    fun oldFun(c: Char): Char = c.newFun(this)
}

fun Char.newFun(x: X): Char = this

fun foo(x: X?, p: Boolean, s: String) {
    val chars = s.filter {
        val v = if (p)
            x?.<caret>oldFun(it)
        else
            null
        v != 'a'
    }
}
