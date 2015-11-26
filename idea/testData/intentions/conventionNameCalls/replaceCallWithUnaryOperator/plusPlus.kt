fun test() {
    class Test {
        operator fun unaryPlus(): Test = Test()
    }
    val test = Test()
    +test.unaryP<caret>lus()
}
