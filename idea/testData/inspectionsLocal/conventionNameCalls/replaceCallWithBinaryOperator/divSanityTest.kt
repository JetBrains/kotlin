// FIX: Replace with '/' operator
fun test() {
    class Test {
        operator fun div(a: Int): Test = Test()
    }
    val test = Test()
    test.<caret>div(1)
}
