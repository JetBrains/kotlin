// TARGET_BACKEND: JVM
// LAMBDAS: CLASS

fun check(expected: String, obj: Any?) {
    val actual = obj.toString()
    if (actual != expected)
        throw AssertionError("Expected: $expected, actual: $actual")
}

fun box(): String {
    check("Function0<kotlin.Unit>",
          { -> })
    check("Function0<java.lang.Integer>",
          { -> 42 })
    check("Function1<java.lang.String, java.lang.Long>",
          fun (s: String) = 42.toLong())
    check("Function2<java.lang.Integer, java.lang.Integer, kotlin.Unit>",
          { x: Int, y: Int -> })

    check("Function1<java.lang.Integer, kotlin.Unit>",
          fun Int.() {})
    check("Function1<kotlin.Unit, java.lang.Integer>",
          fun Unit.(): Int? = 42)
    check("Function2<java.lang.String, java.lang.String, java.lang.Long>",
          fun String.(s: String?): Long = 42.toLong())
    check("Function3<java.util.List<? extends java.lang.String>, java.util.Set<?>, ?, kotlin.Unit>",
          fun List<String>.(x: MutableSet<*>, y: Nothing) {})

    check("Function8<int[], byte[], short[], char[], long[], boolean[], float[], double[], java.lang.Integer[]>",
          fun (ia: IntArray, ba: ByteArray, sa: ShortArray, ca: CharArray, la: LongArray, za: BooleanArray, fa: FloatArray, da: DoubleArray): Array<Int> = null!!)

    check("Function1<java.util.List<? extends java.lang.String>[][][], java.lang.Comparable<? super java.lang.String>>",
          fun (a: Array<Array<Array<List<String>>>>): Comparable<String> = null!!)

    return "OK"
}
