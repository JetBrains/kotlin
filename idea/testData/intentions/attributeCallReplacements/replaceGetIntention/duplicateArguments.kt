// SHOULD_FAIL_WITH: duplicate.or.missing.arguments
fun test() {
    class Test{
        fun get(a: Int, b: Int) : Int = 0
    }
    val test = Test()
    test.g<caret>et(a=0, a=1)
}
