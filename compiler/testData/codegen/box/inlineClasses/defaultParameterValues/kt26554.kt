// IGNORE_BACKEND: WASM
// WASM_MUTE_REASON: STDLIB_GENERATED
// WITH_STDLIB

data class RGBA(val rgba: Int)

@Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")
@kotlin.jvm.JvmInline
value class RgbaArray(val array: IntArray) {
    val size: Int get() = array.size

    fun fill(value: RGBA, start: Int = 0, end: Int = this.size): Unit = array.fill(value.rgba, start, end)
}

fun box(): String {
    val rgbas = RgbaArray(IntArray(10))
    rgbas.fill(RGBA(123456))
    for (i in rgbas.array.indices) {
        if (rgbas.array[i] != 123456) throw AssertionError()
    }
    return "OK"
}