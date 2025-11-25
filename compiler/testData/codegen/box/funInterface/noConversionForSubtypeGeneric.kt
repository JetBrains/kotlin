// IGNORE_BACKEND: JS_IR, JS_IR_ES6
// IGNORE_IR_DESERIALIZATION_TEST: JS_IR, JS_IR_ES6
// IGNORE_BACKEND_K2: ANY
// ISSUE: KT-82590

interface MyInterface

fun interface SimpleRibCoroutineWorker<E> {
    suspend fun onStart(scope: E)
}

class FooWorker<F>(val s: String) : SimpleRibCoroutineWorker<F>, (String) -> Unit {
    override suspend fun onStart(scope: F) {
    }

    override fun toString(): String = s

    override fun invoke(str: String) { /* no op*/ }
}

var result = ""

fun <T1> foo(x: SimpleRibCoroutineWorker<T1>) {
    result += x.toString()
}
fun <T2> bar(vararg x: SimpleRibCoroutineWorker<T2>) {
    result += x[0].toString()
}

fun createNewPlugin(): SimpleRibCoroutineWorker<MyInterface> {
    val x: SimpleRibCoroutineWorker<MyInterface> = FooWorker("O")
    result += x.toString()

    return FooWorker("K")
}

fun box(): String {
    val t = createNewPlugin().toString()
    result += t
    foo<MyInterface>(FooWorker("1"))
    bar<MyInterface>(FooWorker("2"))

    if (result != "OK12") return "fail: $result"
    return "OK"
}