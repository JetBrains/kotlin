class Test<T> {
    val T.foo: String
        get() = this as String
}

fun box(): String {
    with(Test<String>()){
        return "OK".foo
    }
}