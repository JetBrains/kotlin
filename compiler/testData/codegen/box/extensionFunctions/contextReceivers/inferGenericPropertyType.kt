// !LANGUAGE: +ContextReceivers
// TARGET_BACKEND: JVM_IR

class Result<T>(val x: T)

context(Result<T>)
val <T> result: Result<T> get() = this@Result

fun <T> Result<T>.x(): T {
    with(result) {
        return x
    }
}

fun box(): String {
    with(Result<String>("OK")) {
        return x()
    }
}
