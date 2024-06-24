private fun combine(indexToString: (Int) -> String): String {
    return indexToString(0) + indexToString(1)
}

private fun test(): String = run {
    val strings = arrayOf("O", "K")

    fun indexToString(index: Int) = strings[index]

    return@run combine(::indexToString)
}

fun box(): String {
    return test()
}
