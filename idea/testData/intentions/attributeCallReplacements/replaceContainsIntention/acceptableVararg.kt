fun test() {
    class Test{
        fun contains(vararg a: Int, b: Int = 0): Boolean = true
    }
    val test = Test()
    test.contai<caret>ns(1)
}
