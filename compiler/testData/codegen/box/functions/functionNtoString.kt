fun check(expected: String, obj: Any?) {
    val actual = obj.toString()
    if (actual != expected)
        throw AssertionError("Expected: $expected, actual: $actual")
}


fun box(): String {
    check("jet.FunctionImpl0<jet.Unit>")
        { () : Unit -> }
    check("jet.FunctionImpl0<java.lang.Integer>")
        { () : Int -> 42 }
    check("jet.FunctionImpl1<java.lang.String, java.lang.Long>")
        { (s: String) : Long -> 42.toLong() }
    check("jet.FunctionImpl2<java.lang.Integer, java.lang.Integer, jet.Unit>")
        { (x: Int, y: Int) : Unit -> }

    check("jet.ExtensionFunctionImpl0<java.lang.Integer, jet.Unit>")
        { Int.() : Unit -> }
    check("jet.ExtensionFunctionImpl0<jet.Unit, java.lang.Integer>")
        { Unit.() : Int -> 42 }
    check("jet.ExtensionFunctionImpl1<java.lang.String, java.lang.String, java.lang.Long>")
        { String.(s: String) : Long -> 42.toLong() }
    check("jet.ExtensionFunctionImpl2<java.lang.Integer, java.lang.Integer, java.lang.Integer, jet.Unit>")
        { Int.(x: Int, y: Int) : Unit -> }

    return "OK"
}
