fun foo(vararg xs: String) {
}

fun bar(ys: Array<String>) {
    foo(<caret>*arrayOf(*ys, "zzz"))
}