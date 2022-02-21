// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +GenericInlineClassParameter

OPTIONAL_JVM_INLINE_ANNOTATION
value class Result<T: Any>(val a: T?) {
    fun getOrThrow(): T? = a
}

abstract class ResultReceiver<T: Any> {
    abstract fun receive(result: Result<T>)
}

fun <T: Any> ResultReceiver(f: (Result<T>) -> Unit): ResultReceiver<T> =
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