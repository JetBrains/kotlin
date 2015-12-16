// IS_APPLICABLE: false
fun test() {
    class Test {
        operator fun unaryPlus(fn: () -> Unit): Test = Test()
    }
    val test = Test()
    test.unaryPl<caret>us {}
}
