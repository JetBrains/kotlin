fun test() {
    class Test{
        fun get(i: Int) : Int = 0
    }
    val test = Test()
    test.g<caret>et(0)
}
