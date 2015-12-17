fun test() {
    class Test {
        operator fun invoke() {}
    }
    val test = Test()
    test.i<caret>nvoke()
}
