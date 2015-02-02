fun Any.foo() { }

val Any.bar: String get() = ""

fun box(): String {
    return Test.invokeFoo()
}
