fun test() {
    class Test {
        fun plus(a: Int=1): Test = Test()
    }
    val test = Test()
    test.pl<caret>us()
}
