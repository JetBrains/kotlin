// IS_APPLICABLE: false
fun test() {
    class Test {
        operator fun dec(): Test = Test()
    }
    val test = Test()
    test.dec<caret>()
}
