// !LANGUAGE: +ContextReceivers
// TARGET_BACKEND: JVM_IR
// IGNORE_BACKEND_FIR: JVM_IR

class C {
    val result = "OK"
}

fun contextual(f: context(C) () -> String) = f(C())

fun box(): String = contextual { result }