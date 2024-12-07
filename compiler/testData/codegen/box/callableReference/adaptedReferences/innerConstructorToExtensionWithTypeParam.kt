var result = "failed"

class Foo<T> {
    inner class Bar<T> {
        constructor() {
            result = "OK"
        }
    }
}

fun <T> foo(factory: Foo<T>.() -> Foo<T>.Bar<T>) {
    val x: Foo<T>.Bar<T> = factory(Foo())
}


fun box(): String {
    foo(Foo<Int>::Bar)
    return result
}