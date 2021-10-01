// IGNORE_BACKEND: JVM
// IGNORE_BACKEND: WASM
// WASM_MUTE_REASON: SAM_CONVERSIONS

private fun interface Listener {
    fun onChanged(): String
}

private class Foo {
    private val listener = Listener { "OK" }
    val result = listener.onChanged()
}

private val foo = Foo()

fun box(): String = foo.result
