fun test() {
    class Test{
        fun contains(a: Int, b: Int=5) : Boolean = true
    }
    val test = Test()
    test.c<caret>ontains(1)
}
