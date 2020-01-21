class Your {
    class Nested
}

class My {
    fun foo() {
        val x = ::Nested // Should be error
    }
}

fun Your.foo() {
    val x = ::Nested // Still should be error
}