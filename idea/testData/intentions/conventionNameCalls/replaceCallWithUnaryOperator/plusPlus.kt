fun test() {
    class Test {
        fun unaryPlus(): Test = Test()
    }
    val test = Test()
    +test.unaryP<caret>lus()
}
