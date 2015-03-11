// "Create property 'foo'" "true"
// ERROR: Property must be initialized or be abstract

class A<T>(val n: T) {
    default object {
        val foo: Int

    }
}

fun test() {
    val a: Int = A.foo
}
