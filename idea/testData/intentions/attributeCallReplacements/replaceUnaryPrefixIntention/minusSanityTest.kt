// INTENTION_TEXT: Replace with '-' prefix
fun test() {
    class Test {
        fun minus(): Test = Test()
    }
    val test = Test()
    test.min<caret>us()
}
