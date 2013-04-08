class Test {
    fun test() {
        val some : <caret>
    }
}

// EXIST: Any, Nothing, Unit, Int, Number
// EXIST: Array, Hashable
// EXIST_JAVA_ONLY: Math, Thread