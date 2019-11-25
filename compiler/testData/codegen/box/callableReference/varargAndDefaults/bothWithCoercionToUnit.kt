// !LANGUAGE: +NewInference +FunctionReferenceWithDefaultValueAsOtherType
// IGNORE_BACKEND_FIR: JVM_IR
// IGNORE_BACKEND: JS

fun foo(s: String = "kotlin", vararg t: String): Boolean {
    if (s != "kotlin") throw AssertionError(s)
    if (t.size != 0) throw AssertionError(t.size.toString())
    return true
}

fun bar(f: () -> Unit) {
    f()
}

fun box(): String {
    bar(::foo)
    return "OK"
}
