// WITH_STDLIB

@Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")
@kotlin.jvm.JvmInline
value class Rgba(val value: Int) {
    inline val r: Int get() = (value shr 0) and 0xFF
    inline val g: Int get() = (value shr 8) and 0xFF
    inline val b: Int get() = (value shr 16) and 0xFF
    inline val a: Int get() = (value shr 24) and 0xFF
}

fun Rgba(r: Int, g: Int, b: Int, a: Int): Rgba {
    return Rgba(
        ((r and 0xFF) shl 0) or ((g and 0xFF) shl 8) or ((b and 0xFF) shl 16) or ((a and 0xFF) shl 24)
    )
}

fun Rgba.withR(r: Int) = Rgba(r, g, b, a)
fun Rgba.withG(g: Int) = Rgba(r, g, b, a)
fun Rgba.withB(b: Int) = Rgba(r, g, b, a)
fun Rgba.withA(a: Int) = Rgba(r, g, b, a)

@Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")
@kotlin.jvm.JvmInline
value class RgbaArray(val array: IntArray) {
    constructor(size: Int) : this(IntArray(size))
    operator fun get(index: Int): Rgba = Rgba(array[index])
    operator fun set(index: Int, color: Rgba) {
        array[index] = color.value
    }
}

fun box(): String {
    val result1 = RgbaArray(32)
    val result2 = RgbaArray(IntArray(32))
    val color = Rgba(128, 128, 0, 255)
    result1[0] = color.withG(64).withA(0)
    result2[0] = color.withG(64).withA(0)
    if (result1[0].value != result2[0].value) return "Fail 1"
    if (result1[0].value != 16512) return "Fail 2"

    return "OK"
}