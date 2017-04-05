// "Replace with 'c1.newFun(this, c2)'" "true"

class X(val c: Char) {
    @Deprecated("", ReplaceWith("c1.newFun(this, c2)"))
    fun oldFun(c1: Char, c2: Char): Char = c1.newFun(this, c2)
}

fun Char.newFun(x: X, c: Char): Char = this

fun foo(s: String, t: X) {
    val chars = s.filter {
        (X('a') + X('b'))?.<caret>oldFun(it, t.c) != 'a'
    }
}

operator fun X.plus(x: X): X? = null