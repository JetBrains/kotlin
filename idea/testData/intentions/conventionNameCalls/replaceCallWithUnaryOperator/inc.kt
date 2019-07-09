// IS_APPLICABLE: false
fun test() {
    class Test {
        operator fun inc(): Test = Test()
    }
    val test = Test()
    test.inc<caret>()
}
