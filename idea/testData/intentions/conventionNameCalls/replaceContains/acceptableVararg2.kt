fun test() {
    class Test{
        fun contains(c: Int, vararg a: Int, b: Int = 0): Boolean = true
    }
    val test = Test()
    test.contai<caret>ns(1)
}
