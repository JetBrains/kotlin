// FIX: Replace with '..'
fun test() {
    class Test {
        operator fun rangeTo(a: Int): Test = Test()
    }
    val test = Test()
    test.range<caret>To(1)
}
