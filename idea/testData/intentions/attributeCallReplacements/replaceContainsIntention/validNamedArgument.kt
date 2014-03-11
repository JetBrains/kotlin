fun test() {
    class Test{
        fun contains(a: Int=1, b: Int=2) : Boolean = true
    }
    val test = Test()
    test.c<caret>ontains(a=5)
}
