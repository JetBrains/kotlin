// "Convert property initializer to getter" "true"

fun String.foo() = "bar"

interface A {
    val name = <caret>"The quick brown fox jumps over the lazy dog".foo()
}