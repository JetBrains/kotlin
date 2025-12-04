// WITH_STDLIB
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

interface MyDeferred<T> {
    suspend fun await(): T
}

abstract class MyException : Exception() {
    abstract fun isInternal(): Boolean
}

@OptIn(ExperimentalContracts::class)
suspend fun <T> MyDeferred<T>.safeAwait(
    fallbackOnAbort: suspend () -> T,
    onCancelled: suspend (MyException) -> T = { throw it },
) : T {
    contract <expr>{
        callsInPlace(fallbackOnAbort, InvocationKind.AT_MOST_ONCE)
        callsInPlace(onCancelled, InvocationKind.AT_MOST_ONCE)
    }</expr>
    return try {
        await()
    } catch (e: MyException) {
        if (e.isInternal()) {
            fallbackOnAbort()
        } else {
            onCancelled(e)
        }
    }
}