// SHOULD_FAIL_WITH: skipped.defaults
fun test() {
    class Test{
        fun plus(a: Int=1, b: Int=2) : Int = 0
    }
    val test = Test()
    test.p<caret>lus(b=3)
}
