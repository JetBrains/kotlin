// !DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER

suspend fun <T> threadSafeSuspendCallback(startAsync: (CompletionLambda<T>) -> CancellationLambda): T = TODO()
typealias CompletionLambda<T> = (result: Result<T>) -> Unit
typealias CancellationLambda = () -> Unit

class Scope {
    suspend fun <T> performAndWait(block: suspend CoroutineScope.() -> T): T {
        return CoroutineWorker().run {
            val result = threadSafeSuspendCallback<T> { completion ->
                val workItem = WorkItem {
                    val result = runCatching {
                        block()
                    }
                    completion(result)
                }
                return@threadSafeSuspendCallback { Unit }
            }
            result
        }
    }

    class WorkItem(
        val block: suspend CoroutineScope.() -> Unit
    )
}

class CoroutineWorker
interface CoroutineScope