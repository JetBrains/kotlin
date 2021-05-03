val testGlobal = 12

val test : Int get() {
    class SomeMore(testParam : Int) {
        init {
            tes<caret>
        }
    }
}

// EXIST: testGlobal, test, testParam
// FIR_COMPARISON