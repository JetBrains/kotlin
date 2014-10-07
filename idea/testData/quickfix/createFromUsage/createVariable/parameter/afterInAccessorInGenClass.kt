// "Create parameter 'foo'" "true"

class A<T>(val foo: T) {
    val test: T get() {
        return foo
    }
}