// "Replace with 's.newFun(this)'" "true"

class X {
    @Deprecated("", ReplaceWith("s.newFun(this)"))
    fun oldFun(s: String): String = s.newFun(this)
}

fun String.newFun(x: X): String = this

fun foo(x: X?, p: Boolean) {
    val v = if (p)
        x?.<caret>oldFun("a")
    else
        null
}
