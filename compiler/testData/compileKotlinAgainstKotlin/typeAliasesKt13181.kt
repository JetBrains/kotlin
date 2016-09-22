// FILE: A.kt
class Foo<out T>(val t: T) {
    typealias Bar = (T) -> String

    fun baz(b: Bar) = b(t)
}

// FILE: B.kt
class FooTest {
    fun baz(): String {
        val b: Foo<String>.Bar = { "OK" }
        return Foo("").baz(b)
    }
}

fun box(): String =
        FooTest().baz()