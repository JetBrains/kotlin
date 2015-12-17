// IS_APPLICABLE: false
fun test() {
    class Test {
        operator fun <T> div(a: Test): T? = a as? T
    }
    val test = Test()
    test.div<caret><Int>(Test())
}
