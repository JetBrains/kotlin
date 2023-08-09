// !LANGUAGE: +ContextReceivers
// KT-61141: Kotlin/Native does not support context receivers
// IGNORE_BACKEND: NATIVE

fun withContext(f: context(String) () -> String) = f("OK")

fun callWithContext(f: (String) -> String) = withContext(f)

fun box(): String {
    return callWithContext { s -> s }
}

