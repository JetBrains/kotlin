fun test() {
    class Test {
        operator fun plus(a: Int, b: Int=5): Test = Test()
    }
    val test = Test()
    test.pl<caret>us(1)
}
