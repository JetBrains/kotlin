// !DIAGNOSTICS_NUMBER: 3
// !DIAGNOSTICS: TYPE_INFERENCE_TYPE_CONSTRUCTOR_MISMATCH

class A<T, R>
fun <T, R> foo(a: A<T, R>) = a

fun test() {
    foo { it }
    foo { x -> x}
    foo { (x: Int) -> x}
}