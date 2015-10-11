fun test() {
    class Test()

    fun Test.unaryPlus(): Test = Test()

    val test = Test()
    test.unaryPl<caret>us()
}
