val testGlobal = 12

val test : Int get() {
    class SomeMore(testParam : Int) {
        {
            tes<caret>
        }
    }
}

// EXIST: testGlobal, test, testParam
