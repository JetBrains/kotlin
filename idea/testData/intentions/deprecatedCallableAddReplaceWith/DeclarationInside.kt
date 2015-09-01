// IS_APPLICABLE: false
<caret>@Deprecated("")
fun foo(p: Int) {
    if (p > 0) {
        val v = p + 1
    }
}

fun bar(p: Int){}
