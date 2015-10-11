// IS_APPLICABLE: false
fun test() {
    class Test {
        fun unaryPlus<T>(): T? = this as? T
    }
    val test = Test()
    test.unaryP<caret>lus<Int>()
}
