// RUN_PIPELINE_TILL: BACKEND
class A {
    class Nested

    fun main() {
        val x = ::Nested
        val y = A::Nested
    }
}
