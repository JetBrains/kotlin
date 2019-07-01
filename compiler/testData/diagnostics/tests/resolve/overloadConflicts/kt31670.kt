// !WITH_NEW_INFERENCE
// !CONSTRAINT_SYSTEM_FOR_OVERLOAD_RESOLUTION: CONSTRAINT_SYSTEM_FOR_NEW_INFERENCE

open class A<T>(val value: T)
class B<T>(value: T) : A<T>(value)

fun <T> A<T>.foo(block: (T?) -> Unit) {
    block(value)
}
fun <T> B<T>.foo(block: (T) -> Unit) {
    block(value)
}

fun main() {
    B("string").<!NI;OVERLOAD_RESOLUTION_AMBIGUITY!>foo<!> {  }
}