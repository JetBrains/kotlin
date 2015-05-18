// "Replace with 's.filter { it != c }'" "true"

class X {
    @deprecated("", ReplaceWith("s.filter { it != c }"))
    fun oldFun(s: String): CharSequence = s.filter { it != c }

    val c = 'x'
}

fun foo(x: X?, s: String) {
    bar(x?.<caret>oldFun(s))
}

fun bar(s: CharSequence?){}
