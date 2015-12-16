fun test() {
    class Test {
        operator fun plus(fn: () -> Test): Test = fn()
    }
    val test = Test()
    test.pl<caret>us {
        Test()
    }
}
