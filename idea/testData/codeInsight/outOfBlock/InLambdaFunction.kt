// OUT_OF_CODE_BLOCK: FALSE
// TYPE: t

fun CharSequence.repeat(n: Int): String {
    val sb = StringBuilder(n * length)
    for (i in 1..n) {
        sb.append(this)
    }
    return sb.toString()
}

fun twice(s: String): String {
    val repeatFun: String.(Int) -> String = { t -> this.repeat(<caret>) }

    return repeatFun(s, 2)
}
