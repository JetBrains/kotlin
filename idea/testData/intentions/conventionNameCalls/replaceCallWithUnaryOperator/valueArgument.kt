// IS_APPLICABLE: false
fun test() {
    class Test {
        operator fun unaryPlus(a: Int): Test = Test()
    }
    val test = Test()
    test.unaryPl<caret>us(1)
}
