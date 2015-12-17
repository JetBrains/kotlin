// INTENTION_TEXT: Replace with '/' operator
fun test() {
    class Test {
        operator fun div(a: Int): Test = Test()
    }
    val test = Test()
    test.div<caret>(1)
}
