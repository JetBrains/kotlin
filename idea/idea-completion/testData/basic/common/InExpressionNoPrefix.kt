class Test {
    fun test() {
        val some : <caret>
    }
}

// EXIST: Any, Nothing, Unit, Int, Number, Array, Math
// EXIST_JAVA_ONLY: Thread