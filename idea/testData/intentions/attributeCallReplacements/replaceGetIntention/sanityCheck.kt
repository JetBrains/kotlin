// IS_APPLICABLE: false
fun test() {
    class Test{
        fun get(i: Int) : Int = 0
    }
    val test = Test()
    test.g<caret>ot(0)
}
