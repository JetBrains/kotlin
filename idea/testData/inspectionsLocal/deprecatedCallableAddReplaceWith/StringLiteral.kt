<caret>@Deprecated("")
fun foo(p: Int) {
    bar("\"\"\n1\r2\t3")
}

fun bar(s: String){}