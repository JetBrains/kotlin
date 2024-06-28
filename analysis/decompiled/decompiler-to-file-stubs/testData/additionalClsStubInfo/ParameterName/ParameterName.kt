// FIR_IDENTICAL
package test

class ParameterName {
    inline fun <A, B> foo(crossinline block: (input: A, state: B) -> Unit) {}
}