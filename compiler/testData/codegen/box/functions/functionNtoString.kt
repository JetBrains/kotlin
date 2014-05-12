fun check(expected: String, obj: Any?) {
    val actual = obj.toString()
    if (actual != expected)
        throw AssertionError("Expected: $expected, actual: $actual")
}


fun box(): String {
    check("kotlin.Function0<kotlin.Unit>")
        { () : Unit -> }
    check("kotlin.Function0<java.lang.Integer>")
        { () : Int -> 42 }
    check("kotlin.Function1<java.lang.String, java.lang.Long>")
        { (s: String) : Long -> 42.toLong() }
    check("kotlin.Function2<java.lang.Integer, java.lang.Integer, kotlin.Unit>")
        { (x: Int, y: Int) : Unit -> }

    check("kotlin.ExtensionFunction0<java.lang.Integer, kotlin.Unit>")
        { Int.() : Unit -> }
    check("kotlin.ExtensionFunction0<kotlin.Unit, java.lang.Integer>")
        { Unit.() : Int -> 42 }
    check("kotlin.ExtensionFunction1<java.lang.String, java.lang.String, java.lang.Long>")
        { String.(s: String) : Long -> 42.toLong() }
    check("kotlin.ExtensionFunction2<java.lang.Integer, java.lang.Integer, java.lang.Integer, kotlin.Unit>")
        { Int.(x: Int, y: Int) : Unit -> }

    return "OK"
}
