fun test() {
    class Test{
        operator fun <T> contains(a: T): Boolean = false
    }
    val test = Test()
    test.contai<caret>ns<Int>(1)
}
