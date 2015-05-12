// IS_APPLICABLE: false
fun test() {
    class Test {
        fun plus(a: Int, b: Int): Test = Test()
    }
    val test = Test()
    test.pl<caret>us(1, 2)
}
