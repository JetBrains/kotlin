// IS_APPLICABLE: false
fun test() {
    class Test {
        fun unaryPlus(a: Int): Test = Test()
    }
    val test = Test()
    test.unaryPl<caret>us(a=1)
}
