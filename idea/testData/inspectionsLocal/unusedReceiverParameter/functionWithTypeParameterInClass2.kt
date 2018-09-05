class Test<T> {
    fun <U> <caret>U.foo() {}

    fun test() {
        "".foo()
    }
}