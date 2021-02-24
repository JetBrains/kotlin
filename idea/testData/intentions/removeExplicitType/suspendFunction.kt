// IS_APPLICABLE: false
fun test() {
    val x: suspend (x: Int, y: Int) -> Unit<caret> = { x: Int, y: Int ->
    }
}