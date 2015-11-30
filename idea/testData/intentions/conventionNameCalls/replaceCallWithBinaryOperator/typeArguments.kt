// IS_APPLICABLE: false
fun test() {
    class Test {
        fun <T> div(a: Test): T? = a as? T
    }
    val test = Test()
    test.div<caret><Int>(Test())
}
