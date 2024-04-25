// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_VARIABLE
// OPT_IN: kotlin.RequiresOptIn

import kotlin.experimental.ExperimentalTypeInference

interface Inv<T> {
    fun send(e: T)
}

@OptIn(ExperimentalTypeInference::class)
fun <K> foo(block: Inv<K>.() -> Unit) {}

fun test(i: Int) {
    foo {
        val p = send(i)
    }
}