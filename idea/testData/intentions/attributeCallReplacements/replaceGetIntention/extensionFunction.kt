fun test() {
    class Test()

    fun Test.get(i: Int) : Int = 0
    val test = Test()
    test.g<caret>et(0)
}
