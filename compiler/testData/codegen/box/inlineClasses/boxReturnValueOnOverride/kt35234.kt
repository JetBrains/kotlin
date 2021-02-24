// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: STDLIB_TEXT
// WITH_RUNTIME
// KJS_WITH_FULL_RUNTIME

inline class NumberInlineClass(val value: Double)

interface TypeAdapter<FROM, TO> {
    fun decode(string: FROM): TO
}

class StringToDoubleTypeAdapter : TypeAdapter<String, NumberInlineClass> {
    override fun decode(string: String) = NumberInlineClass(string.toDouble())
}

fun box(): String {
    val typeAdapter = StringToDoubleTypeAdapter()
    val test = typeAdapter.decode("2019")
    if (test.value != 2019.0) throw AssertionError("test: $test")
    return "OK"
}