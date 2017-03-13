// FILE: A.kt
typealias Bar<T> = (T) -> String

class Foo<out T>(val t: T) {

    fun baz(b: Bar<T>) = b(t)
}

// FILE: B.kt
class FooTest {
    fun baz(): String {
        val b: Bar<String> = { "OK" }
        return Foo("").baz(b)
    }
}

fun box(): String =
        FooTest().baz()