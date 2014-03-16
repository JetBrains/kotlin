fun test() {
    class Test {
        fun invoke(fn: () -> Unit) {}
    }
    val test = Test()
    test.i<caret>nvoke {

    }
}
