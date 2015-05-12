fun test() {
    class Test{
        fun contains<T>(a: T): Boolean = false
    }
    val test = Test()
    test.contai<caret>ns<Int>(1)
}
