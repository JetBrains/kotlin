// LANGUAGE: +ContextReceivers, -ContextParameters
// IGNORE_BACKEND_K2: ANY

fun withContext(f: context(String) () -> String) = f("OK")

fun callWithContext(f: (String) -> String) = withContext(f)

fun box(): String {
    return callWithContext { s -> s }
}

