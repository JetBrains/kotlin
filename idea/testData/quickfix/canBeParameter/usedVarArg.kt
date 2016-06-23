// "Remove 'val' from parameter" "true"

class Wrapper(vararg <caret>val x: Int) {
    val y = x
}
