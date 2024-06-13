// LANGUAGE: +ContextReceivers
// TARGET_BACKEND: JVM_IR
// FIR status: context receivers aren't yet supported

class A {
    val result = "OK"
}

fun <T> test(receiver: T, action: context(T) () -> String) = action(receiver)

fun box(): String = with(A()) {
    result
}
