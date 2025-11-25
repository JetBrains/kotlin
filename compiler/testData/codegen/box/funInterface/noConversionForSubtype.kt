// IGNORE_BACKEND: JS_IR, JS_IR_ES6
// IGNORE_IR_DESERIALIZATION_TEST: JS_IR, JS_IR_ES6
// ISSUE: KT-82590

interface MyInterface

fun interface SimpleRibCoroutineWorker {
    suspend fun onStart(scope: MyInterface)
}

class FooWorker(val s: String) : SimpleRibCoroutineWorker, (String) -> Unit {
    override suspend fun onStart(scope: MyInterface) {
    }

    override fun toString(): String = s

    override fun invoke(str: String) { /* no op*/ }
}

var result = ""

fun foo(x: SimpleRibCoroutineWorker) {
    result += x.toString()
}
fun bar(vararg x: SimpleRibCoroutineWorker) {
    result += x[0].toString()
}

fun createNewPlugin(): SimpleRibCoroutineWorker {
    val x: SimpleRibCoroutineWorker = FooWorker("O")
    result += x.toString()

    return FooWorker("K")
}

fun box(): String {
    val t = createNewPlugin().toString()
    result += t
    foo(FooWorker("1"))
    bar(FooWorker("2"))

    if (result != "OK12") return "fail: $result"
    return "OK"
}
