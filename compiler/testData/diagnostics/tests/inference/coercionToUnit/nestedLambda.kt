// FIR_IDENTICAL
// !SKIP_TXT
interface Context<T> {
    fun proceed(): T
}

interface A<T : Any, K : Any> {
    fun process(block: Context<T>.(T) -> Unit)
}

fun <T> processNested(body: () -> T): T {
    return body()
}

fun test(pipeline: A<Any, String>) {
    pipeline.process {
        // OK: processNested<Unit> { /* no return */ proceed() /*: Any */ /* implicit coercion to Unit */ }
        return@process processNested { proceed() }
    }
}
