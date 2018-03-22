// IS_APPLICABLE: false
class Test {
    /* foo */ // bar
    private val x: Int <caret>/* baz */ // qux
    init {
        x = 1
    }
}