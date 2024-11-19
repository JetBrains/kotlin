// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// LANGUAGE: -CheckLambdaAgainstTypeVariableContradictionInResolution
interface MyProperty<E>

fun <T> foo(property: MyProperty<T>, value: T) {}
fun <T> foo(property: MyProperty<T>, predicate: (T) -> Boolean) {}

fun bar(x: MyProperty<String>) {
    foo(x) {
        it.length == 0
    }
}
