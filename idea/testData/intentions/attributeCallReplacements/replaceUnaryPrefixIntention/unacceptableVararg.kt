// IS_APPLICABLE: false
fun test() {
    class Test {
        fun plus(vararg a: Int): Test = Test()
    }
    val test = Test()
    test.pl<caret>us(0)
}
