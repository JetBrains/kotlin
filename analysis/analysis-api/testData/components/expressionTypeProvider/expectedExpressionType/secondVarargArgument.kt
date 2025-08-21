fun main() {
    val array: Array<(String) -> Unit> = arrayOf(
        { _ -> },
        { it<caret> -> }
    )
}
