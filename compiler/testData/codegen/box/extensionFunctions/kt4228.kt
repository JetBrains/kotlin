class A {
    default object
}

val foo: Any.() -> Unit = {}

fun test() {
    A.(foo)()
}

fun box(): String {
    test()
    return "OK"
}
