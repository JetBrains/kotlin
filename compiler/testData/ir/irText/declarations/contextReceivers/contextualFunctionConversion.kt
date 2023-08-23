// !LANGUAGE: +ContextReceivers
// KT-61141: K1/Native does not support context receivers
// IGNORE_BACKEND_K1: NATIVE

fun withContext(f: context(String) () -> String) = f("OK")

fun callWithContext(f: (String) -> String) = withContext(f)

fun box(): String {
    return callWithContext { s -> s }
}

