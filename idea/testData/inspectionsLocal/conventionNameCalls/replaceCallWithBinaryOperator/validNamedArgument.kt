fun test() {
    class Test {
        operator fun plus(a: Int): Test = Test()
    }
    val test = Test()
    test.pl<caret>us(a=1)
}
