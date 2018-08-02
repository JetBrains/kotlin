// !LANGUAGE: +InlineClasses
// IGNORE_BACKEND: JVM_IR

inline class SuccessOrFailure<T>(val a: Any?) {
    fun getOrThrow(): T = a as T
}

abstract class SuccessOrFailureReceiver<T> {
    abstract fun receive(result: SuccessOrFailure<T>)
}

inline fun <T> SuccessOrFailureReceiver(crossinline f: (SuccessOrFailure<T>) -> Unit): SuccessOrFailureReceiver<T> =
    object : SuccessOrFailureReceiver<T>() {
        override fun receive(result: SuccessOrFailure<T>) {
            f(result)
        }
    }

fun test() {
    var invoked = false
    val receiver = SuccessOrFailureReceiver<String> { result ->
        val intResult = result.getOrThrow()
        invoked = true
    }

    receiver.receive(SuccessOrFailure("42"))
    if (!invoked) {
        throw RuntimeException("Fail")
    }
}

fun box(): String {
    test()
    return "OK"
}