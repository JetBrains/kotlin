enum class My(x: Int) {
    FIRST: My(13)

    val y = x + 1
}
// COMPILATION_ERRORS
