// LANGUAGE: +ContextParameters
// TARGET_BACKEND: WASM

external interface Context {
    fun action(): String
}

fun get(): Context =
    js("({ action: () => 'OK' })")

context(c: Context)
fun test(): String = c.action()

fun usage1(c: Context): String =
    context(c) { test() }

fun usage2(c: Context): String =
    with(c) { test() }

context(c: Context)
fun usage3(): String = test()

fun box(): String {
    val ctx = get()

    val r1 = usage1(ctx)
    val r2 = usage2(ctx)
    val r3 = context(ctx) { usage3() }

    return if (r1 == "OK" && r2 == "OK" && r3 == "OK") "OK"
    else "FAIL"
}
