class Test {
    fun test() {
        val some : <caret>
    }
}

// EXIST: Any, Nothing, Tuple0, Int, Number
// EXIST: Array, Math, Hashable, OutputStream