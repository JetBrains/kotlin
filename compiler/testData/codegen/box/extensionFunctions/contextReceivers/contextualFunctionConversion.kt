// LANGUAGE: +ContextReceivers
// TARGET_BACKEND: JVM_IR

fun withContext(f: context(String) () -> String) = f("OK")

fun callWithContext(f: (String) -> String) = withContext(f)

fun box(): String {
    return callWithContext { s -> s }
}

