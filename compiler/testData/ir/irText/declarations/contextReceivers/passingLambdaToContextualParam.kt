// IGNORE_BACKEND_K2: ANY
// LANGUAGE: +ContextReceivers, -ContextParameters

class C {
    val result = "OK"
}

fun contextual(f: context(C) () -> String) = f(C())

fun box(): String = contextual { result }
