fun test() {
    class Test{
        operator fun get(a: Int, b: Int) : Int = 0
    }
    val test = Test()
    test.g<caret>et(1, 2)
}
