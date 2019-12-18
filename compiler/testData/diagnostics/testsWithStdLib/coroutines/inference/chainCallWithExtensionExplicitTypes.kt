// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_PARAMETER
// !USE_EXPERIMENTAL: kotlin.RequiresOptIn

import kotlin.experimental.ExperimentalTypeInference

class Inv<K>(val value: K)

interface ProducerScope<E> {
    val prop: Inv<E>
}
class CoroutineScope
class ReceiveChannel<E>

@OptIn(ExperimentalTypeInference::class)
public fun <E> produce(@BuilderInference block: suspend ProducerScope<E>.() -> Unit): ProducerScope<E> = TODO()

fun test(ls: List<Int>) =
    produce<Int> {
        ls.asReceiveChannel().toChannel(prop)
    }

private fun <E> Iterable<E>.asReceiveChannel(): ReceiveChannel<E> = TODO()
public suspend fun <E, C> ReceiveChannel<E>.toChannel(destination: C): C = TODO()