// LANGUAGE: +ContextParameters
// IGNORE_BACKEND_K1: WASM
// TARGET_BACKEND: WASM

external interface Context {
    fun action(): String
}

fun getContext(): Context = js("({ action: () => 'OK' })")

context(c: Context)
fun test(): String = c.action()

fun box(): String {
    val ctx = getContext()
    return context(ctx) {
        test()
    }
}