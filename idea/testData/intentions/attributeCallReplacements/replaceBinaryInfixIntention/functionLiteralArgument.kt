fun test() {
    class Test {
        fun plus(fn: () -> Test): Test = fn()
    }
    val test = Test()
    test.pl<caret>us {
        Test()
    }
}
