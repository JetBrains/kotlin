fun test() {
    class Test()
    operator fun Test.div(a: Int): Test = Test()
    val test = Test()
    test.div<caret>(1)
}
