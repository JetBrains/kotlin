// IS_APPLICABLE: false
fun test() {
    class Test {
        fun unaryPlus(fn: () -> Unit): Test = Test()
    }
    val test = Test()
    test.unaryPl<caret>us {}
}
