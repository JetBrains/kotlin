// IS_APPLICABLE: false
class Test {
    /* foo */ // bar<caret>
    private val x: Int /* baz */ // qux
    init {
        x = 1
    }
}