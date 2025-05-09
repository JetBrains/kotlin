// LANGUAGE: +ContextParameters
// IGNORE_BACKEND_K1: WASM
// TARGET_BACKEND: WASM

external interface I {
    val x: Int
}

@JsFun("() => ({ x: 123 })")
external fun getData(): I

interface Context {
    fun action(data: I): String
}

class Impl : Context {
    override fun action(data: I): String = "Value: ${data.x}"
}

context(c: Context)
fun test(): String {
    val data = getData()
    return c.action(data)
}

fun usage1(c: Context): String =
    context(c) { test() }

fun usage2(c: Context): String =
    with(c) {
        context(this) { test() }
    }

fun box(): String {
    val ctx = Impl()

    val r1 = usage1(ctx)
    val r2 = usage2(ctx)

    return if (r1 == "Value: 123" && r2 == "Value: 123") "OK"
    else "FAIL"
}