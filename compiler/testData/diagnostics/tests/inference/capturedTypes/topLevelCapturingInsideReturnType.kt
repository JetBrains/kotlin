// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_PARAMETER

class Inv2<T, K>

fun <K> createInv(): Inv2<*, K> = TODO()

fun <T> foo(i: Inv2<T, String>) {}

fun foo() {
    foo(createInv())
}