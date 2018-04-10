<caret>@Deprecated("")
fun foo(p: Int) {
    if (p > 0)
        bar1(p)
    else
        bar2(p)
}

fun bar1(p: Int){}
fun bar2(p: Int){}