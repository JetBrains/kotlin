fun Any?.foo() {}

fun test(a: Any?) {
    if (a != null) {
        a.foo()
    }
}