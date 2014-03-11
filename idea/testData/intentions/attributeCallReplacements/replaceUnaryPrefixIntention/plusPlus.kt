fun test() {
    class Test {
        fun plus(): Test = Test()
    }
    val test = Test()
    +test.p<caret>lus()
}
