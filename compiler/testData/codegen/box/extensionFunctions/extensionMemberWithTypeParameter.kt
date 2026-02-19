class Test<T> {
    fun T.foo(): String {
        return this as String
    }
}

fun box(): String {
    with(Test<String>()) {
        return "OK".foo()
    }
}