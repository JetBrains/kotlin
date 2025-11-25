// DONT_TARGET_EXACT_BACKEND: JS_IR
// ISSUE: KT-82590

interface MyInterface

fun interface SimpleRibCoroutineWorker<E> {
    suspend fun onStart(scope: E)
}

class FooWorker<F> : SimpleRibCoroutineWorker<F>, (String) -> Unit {
    override suspend fun onStart(scope: F) {
    }

    override fun invoke(str: String) { /* no op*/ }
}

fun <T1> foo(x: SimpleRibCoroutineWorker<T1>) {}
fun <T2> bar(vararg x: SimpleRibCoroutineWorker<T2>) {}

fun createNewPlugin(): SimpleRibCoroutineWorker<MyInterface> {
    val x: SimpleRibCoroutineWorker<MyInterface> = FooWorker()

    return FooWorker()
}

fun main() {
    foo<MyInterface>(FooWorker())
    bar<MyInterface>(FooWorker())
}