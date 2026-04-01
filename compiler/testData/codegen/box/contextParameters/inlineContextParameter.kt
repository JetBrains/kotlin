// IGNORE_BACKEND_K1: ANY
// LANGUAGE: +ContextParameters +ExplicitContextArguments

// FILE: lib.kt
context(noinline ctx: () -> Unit) inline fun foo() {
    val ctxSink = ctx
}

// FILE: main.kt
fun print(s: String) { }

fun box(): String {
    foo(ctx = { print("OK") })

    val f: () -> Unit = { print("OK") }
    context(f) {
        foo()
    }

    return "OK"
}