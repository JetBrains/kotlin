// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_VARIABLE
// !USE_EXPERIMENTAL: kotlin.Experimental

import kotlin.experimental.ExperimentalTypeInference

interface Inv<T> {
    fun send(e: T)
}

@UseExperimental(ExperimentalTypeInference::class)
fun <K> foo(@BuilderInference block: Inv<K>.() -> Unit) {}

fun test(i: Int) {
    foo {
        val p = send(i)
    }
}