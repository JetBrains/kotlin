// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_PARAMETER

class Inv<T, K>

fun <K> createInv(): Inv<*, K> = TODO()

fun <T> foo(i: Inv<T, String>) {}

fun foo() {
    foo(<!NEW_INFERENCE_ERROR!>createInv()<!>)
}