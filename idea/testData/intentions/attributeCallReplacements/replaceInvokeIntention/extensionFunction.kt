fun test() {
    class Test()

    fun Test.invoke(): Unit = Unit.VALUE

    val test = Test()
    test.i<caret>nvoke()
}
