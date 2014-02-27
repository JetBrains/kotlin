// SHOULD_FAIL_WITH: skipped.defaults
fun test() {
    class Test{
        fun get(a: Int=1, b: Int=2) : Int = 0
    }
    val test = Test()
    test.g<caret>et(b=3)
}
