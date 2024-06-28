// WITH_STDLIB

import kotlin.coroutines.*
import kotlin.experimental.*

interface CoroutineScope

@OptIn(ExperimentalTypeInference::class)
public fun <E> CoroutineScope.produce(
    block: suspend ProducerScope<E>.() -> Unit
): ReceiveChannel<E>  = TODO()

interface ProducerScope<in E> : CoroutineScope, SendChannel<E> {
    public val channel: SendChannel<E>
}

interface ReceiveChannel<out E>
interface SelectBuilder<in R> {
    operator fun <P, Q> SelectClause2<P, Q>.invoke(param: P, block: suspend (Q) -> R)
}
interface SelectClause2<in P, out Q>
interface SendChannel<in E> {
    val onSend: SelectClause2<E, SendChannel<E>>
}
suspend inline fun <R> select(crossinline builder: SelectBuilder<R>.() -> Unit): R {
    TODO()
}

fun CoroutineScope.produceNumbers(side: SendChannel<Int>) = produce<Int> {
    select<Unit> {
        onSend(1) {}
    }
}
