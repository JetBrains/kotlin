// !LANGUAGE: +InlineClasses
// IGNORE_BACKEND: JVM_IR

inline class Result<T>(val a: Any?) {
    fun getOrThrow(): T = a as T
}

abstract class ResultReceiver<T> {
    abstract fun receive(result: Result<T>)
}

fun <T> ResultReceiver(f: (Result<T>) -> Unit): ResultReceiver<T> =
    object : ResultReceiver<T>() {
        override fun receive(result: Result<T>) {
            f(result)
        }
    }

fun test() {
    var invoked = false
    val receiver = ResultReceiver<Int> { result ->
        val intResult = result.getOrThrow()
        invoked = true
    }

    receiver.receive(Result(42))
    if (!invoked) {
        throw RuntimeException("Fail")
    }
}

fun box(): String {
    test()
    return "OK"
}