// LAMBDAS: CLASS
// TARGET_BACKEND: JVM
// TARGET_BACKEND: JVM_IR

// WITH_REFLECT

fun check(expected: String, obj: Any?) {
    val actual = obj.toString()
    if (actual != expected)
        throw AssertionError("Expected: $expected, actual: $actual")
}

fun box(): String {
    check("() -> kotlin.Unit",
          { -> })
    check("() -> kotlin.Int",
          { -> 42 })
    check("(kotlin.String) -> kotlin.Long",
          fun (s: String) = 42.toLong())
    check("(kotlin.Int, kotlin.Int) -> kotlin.Unit",
          { x: Int, y: Int -> })

    check("kotlin.Int.() -> kotlin.Unit",
          fun Int.() {})
    check("kotlin.Unit.() -> kotlin.Int?",
          fun Unit.(): Int? = 42)
    check("kotlin.String.(kotlin.String?) -> kotlin.Long",
          fun String.(s: String?): Long = 42.toLong())
    check("kotlin.collections.List<kotlin.String>.(kotlin.collections.MutableSet<*>, kotlin.Nothing) -> kotlin.Unit",
          fun List<String>.(x: MutableSet<*>, y: Nothing) {})

    check("(kotlin.IntArray, kotlin.ByteArray, kotlin.ShortArray, kotlin.CharArray, kotlin.LongArray, kotlin.BooleanArray, kotlin.FloatArray, kotlin.DoubleArray) -> kotlin.Array<kotlin.Int>",
          fun (ia: IntArray, ba: ByteArray, sa: ShortArray, ca: CharArray, la: LongArray, za: BooleanArray, fa: FloatArray, da: DoubleArray): Array<Int> = null!!)

    check("(kotlin.Array<kotlin.Array<kotlin.Array<kotlin.collections.List<kotlin.String>>>>) -> kotlin.Comparable<kotlin.String>",
          fun (a: Array<Array<Array<List<String>>>>): Comparable<String> = null!!)

    return "OK"
}
