// !LANGUAGE: +ContextReceivers
// TARGET_BACKEND: JVM_IR
// IGNORE_BACKEND_FIR: JVM_IR

fun withContext(f: context(String) () -> String) = f("OK")

fun callWithContext(f: (String) -> String) = withContext(f)

fun box(): String {
    return callWithContext { s -> s }
}

