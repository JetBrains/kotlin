fun test() {
    class Test{
        fun get(a: Int, vararg b: Int, c: Int = 0) : Int = 0
    }
    val test = Test()
    test.g<caret>et(1, 3, 4, 5)
}
