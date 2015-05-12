// IS_APPLICABLE: false
fun test() {
    class Test {
        fun plus<T>(): T? = this as? T
    }
    val test = Test()
    test.p<caret>lus<Int>()
}
