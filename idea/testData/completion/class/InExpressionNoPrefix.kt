class Test {
    fun test() {
        val some : <caret>
    }
}

// RUNTIME: 1
// EXIST: Any, Nothing, Tuple0, Int, Number
// EXIST: Array, Math, Hashable, OutputStream