abstract class Outer {
    interface Nested {
        fun bar()
    }
}

fun main() {
    object : Outer() {
        fun foo(n: Nested) {
            n.bar()
        }
    }
}

class Impl : Outer() {
    fun foo(n: Nested) {
        n.bar()
    }
}
