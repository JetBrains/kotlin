fun test() {
    class Test {
        operator fun <T> invoke(fn: () -> T) {}
    }
    val test = Test()
    test.i<caret>nvoke<Int> { 0 }
}
