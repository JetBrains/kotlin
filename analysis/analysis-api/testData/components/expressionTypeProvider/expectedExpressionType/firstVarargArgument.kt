fun main() {
    val array: Array<(String) -> Unit> = arrayOf(
        { it<caret> -> },
        { _ -> }
    )
}
