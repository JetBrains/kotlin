fun check(expected: String, obj: Any?) {
    val actual = obj.toString()
    if (actual != expected)
        throw AssertionError("Expected: $expected, actual: $actual")
}


fun box(): String {
    check("kotlin.jvm.functions.Function0<kotlin.Unit>")
    { -> }
    check("kotlin.jvm.functions.Function0<java.lang.Integer>")
    { -> 42 }
    check("kotlin.jvm.functions.Function1<java.lang.String, java.lang.Long>",
          fun (s: String) = 42.toLong())
    check("kotlin.jvm.functions.Function2<java.lang.Integer, java.lang.Integer, kotlin.Unit>")
    { x: Int, y: Int -> }

    check("kotlin.ExtensionFunction0<java.lang.Integer, kotlin.Unit>",
          fun Int.() {})
    check("kotlin.ExtensionFunction0<kotlin.Unit, java.lang.Integer>",
          fun Unit.(): Int = 42)
    check("kotlin.ExtensionFunction1<java.lang.String, java.lang.String, java.lang.Long>",
          fun String.(s: String): Long = 42.toLong())
    check("kotlin.ExtensionFunction2<java.lang.Integer, java.lang.Integer, java.lang.Integer, kotlin.Unit>",
          fun Int.(x: Int, y: Int) {})

    return "OK"
}
