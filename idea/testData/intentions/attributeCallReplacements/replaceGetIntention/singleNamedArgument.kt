fun test() {
    class Test{
        fun get(a: Int) : Int = 0
    }
    val test = Test()
    test.g<caret>et(a=1)
}
