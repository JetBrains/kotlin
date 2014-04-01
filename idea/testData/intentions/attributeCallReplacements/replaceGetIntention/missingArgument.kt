// SHOULD_FAIL_WITH: duplicate.or.missing.arguments
// ERROR: No value passed for parameter b
fun test() {
    class Test{
        fun get(a: Int, b: Int) : Int = 0
    }
    val test = Test()
    test.g<caret>et(0)
}
