// OUT_OF_CODE_BLOCK: FALSE

fun twice(s: String): String {
    val repeatFun: String.(Int) -> String = { t -> this.repeat(<caret>) }

    return repeatFun(s, 2)
}

// TYPE: t
