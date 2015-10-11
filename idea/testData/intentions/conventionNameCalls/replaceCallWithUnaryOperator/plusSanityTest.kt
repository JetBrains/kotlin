// INTENTION_TEXT: Replace with '+' operator
fun test() {
    class Test {
        fun unaryPlus(): Test = Test()
    }
    val test = Test()
    test.unaryPl<caret>us()
}
