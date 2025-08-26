interface A<T>

class B: A<Int>

fun A<in Nothing>.foo() {}

/**
 * [B.f<caret_1>oo]
 */
fun main() {
    B().foo()
}