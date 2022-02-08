// !LANGUAGE: +ContextReceivers
// TARGET_BACKEND: JVM_IR
// IGNORE_BACKEND_FIR: JVM_IR
// FIR status: context receivers aren't yet supported

class C {
    val result = "OK"
}

fun contextual(f: context(C) () -> String) = f(C())

fun box(): String = contextual { result }