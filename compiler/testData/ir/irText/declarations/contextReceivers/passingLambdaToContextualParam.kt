// !LANGUAGE: +ContextReceivers
// KT-61141: Kotlin/Native does not support context receivers
// IGNORE_BACKEND_K1: NATIVE

class C {
    val result = "OK"
}

fun contextual(f: context(C) () -> String) = f(C())

fun box(): String = contextual { result }
