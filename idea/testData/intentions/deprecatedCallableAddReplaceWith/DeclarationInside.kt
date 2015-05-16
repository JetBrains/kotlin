// IS_APPLICABLE: false
<caret>@deprecated("")
fun foo(p: Int) {
    if (p > 0) {
        val v = p + 1
        bar(v)
    }
}

fun bar(p: Int){}
