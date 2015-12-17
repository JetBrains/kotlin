// INTENTION_TEXT: Replace with '*' operator
fun test() {
    class Test {
        operator fun times(a: Int): Test = Test()
    }
    val test = Test()
    test.time<caret>s(1)
}
