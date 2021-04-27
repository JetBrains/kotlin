// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_PARAMETER -EXPERIMENTAL_IS_NOT_ENABLED
// ISSUE: KT-41164

import kotlin.experimental.ExperimentalTypeInference

interface MyProducerScope<in E>
interface MyFlow<out T>

fun <K> select(x: K, y: K): K = x

@OptIn(ExperimentalTypeInference::class)
fun <T> myCallbackFlow(@BuilderInference block: MyProducerScope<T>.() -> Unit): MyFlow<T> = null!!

fun MyProducerScope<*>.myAwaitClose(block: () -> Unit = {}) {}
fun <T> myEmptyFlow(): MyFlow<T> = null!!

fun test(): MyFlow<Int> {
    return <!TYPE_MISMATCH!>select(
        <!DEBUG_INFO_EXPRESSION_TYPE("MyFlow<kotlin.Any?>")!>myCallbackFlow <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function1<MyProducerScope<kotlin.Any?>, kotlin.Unit>")!>{
            myAwaitClose {}
        }<!><!>,
        myEmptyFlow()
    )<!>
}
