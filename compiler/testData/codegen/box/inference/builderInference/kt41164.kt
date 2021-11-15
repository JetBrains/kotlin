// ISSUE: KT-41164
// WITH_STDLIB

import kotlin.experimental.ExperimentalTypeInference

interface MyProducerScope<in E>
interface MyFlow<out T>

fun <K> select(x: K, y: K): K = x
@OptIn(ExperimentalTypeInference::class)
fun <T> myCallbackFlow(@BuilderInference block: MyProducerScope<T>.() -> Unit): MyFlow<T> = null!!
fun MyProducerScope<*>.myAwaitClose(block: () -> Unit = {}) {}
fun <E> myEmptyFlow(): MyFlow<E> = null!!

fun test(): MyFlow<Int> {
    return select(
        myCallbackFlow {
            myAwaitClose {}
        },
        myEmptyFlow()
    )
}

fun box(): String = "OK"
