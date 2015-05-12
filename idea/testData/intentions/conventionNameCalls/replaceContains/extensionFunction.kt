fun test() {
    class Test()
    fun Test.contains(a: Int) : Boolean = true
    val test = Test()
    test.c<caret>ontains(1)
}
