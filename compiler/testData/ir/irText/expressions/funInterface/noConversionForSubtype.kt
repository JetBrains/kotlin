// FIR_IDENTICAL
// DONT_TARGET_EXACT_BACKEND: JS_IR
// ISSUE: KT-82590

// KT-42199
// SKIP_NEW_KOTLIN_REFLECT_COMPATIBILITY_CHECK

interface MyInterface

fun interface SimpleRibCoroutineWorker {
    suspend fun onStart(scope: MyInterface)
}

class FooWorker : SimpleRibCoroutineWorker, (String) -> Unit {
    override suspend fun onStart(scope: MyInterface) {
    }

    override fun invoke(str: String) { /* no op*/ }
}

fun foo(x: SimpleRibCoroutineWorker) {}
fun bar(vararg x: SimpleRibCoroutineWorker) {}

fun createNewPlugin(): SimpleRibCoroutineWorker {
    val x: SimpleRibCoroutineWorker = FooWorker()

    return FooWorker()
}

fun main() {
    foo(FooWorker())
    bar(FooWorker())
}