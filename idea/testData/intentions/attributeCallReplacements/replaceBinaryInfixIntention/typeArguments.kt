// IS_APPLICABLE: false
fun test() {
    class Test {
        fun div<T>(a: Test): T? = a as? T
    }
    val test = Test()
    test.div<caret><Int>(Test())
}
