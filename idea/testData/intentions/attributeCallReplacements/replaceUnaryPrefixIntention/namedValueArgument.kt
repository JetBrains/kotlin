// IS_APPLICABLE: false
fun test() {
    class Test {
        fun plus(a: Int): Test = Test()
    }
    val test = Test()
    test.pl<caret>us(a=1)
}
