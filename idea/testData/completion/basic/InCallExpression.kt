package something

class SomeTempClass {
    fun testSome() {

    }

    fun test() {
        test<caret>()
    }
}

// EXIST: test, testSome