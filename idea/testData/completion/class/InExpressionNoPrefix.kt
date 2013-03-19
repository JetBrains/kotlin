class Test {
    fun test() {
        val some : <caret>
    }
}

// RUNTIME: 1
// EXIST: Any, Nothing, Unit, Int, Number
// EXIST: Array, Math, Hashable, OutputStream