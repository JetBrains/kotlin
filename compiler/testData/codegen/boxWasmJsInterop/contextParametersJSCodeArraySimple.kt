// LANGUAGE: +ContextParameters
// TARGET_BACKEND: WASM
// FILE: jsContextInterop.kt

external interface I {
    val x: Int
}

fun getData(): I = js("({ x: 123 })")

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

fun usage1(c: Context): String {
    return context(c) {
        test()
    }
}

fun usage2(c: Context): String {
    return with(c) {
        context(this) {
            test()
        }
    }
}

fun box(): String {
    val ctx = Impl()
    val r1 = usage1(ctx)
    val r2 = usage2(ctx)

    return if (r1 == "Value: 123" && r2 == "Value: 123") "OK"
    else "FAIL"
}