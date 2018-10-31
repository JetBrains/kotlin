// !LANGUAGE: +InlineClasses
// IGNORE_BACKEND: JS, JS_IR, JVM_IR, NATIVE
// WITH_RUNTIME

data class RGBA(val rgba: Int)

inline class RgbaArray(val array: IntArray) {
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