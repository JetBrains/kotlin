fun check(expected: String, obj: Any?) {
    val actual = obj.toString()
    if (actual != expected)
        throw AssertionError("Expected: $expected, actual: $actual")
}


fun box(): String {
    check("jet.FunctionImpl0<? extends jet.Unit>")
        { () : Unit -> }
    check("jet.FunctionImpl0<? extends java.lang.Integer>")
        { () : Int -> 42 }
    check("jet.FunctionImpl1<? super java.lang.String, ? extends java.lang.Long>")
        { (s: String) : Long -> 42.toLong() }
    check("jet.FunctionImpl2<? super java.lang.Integer, ? super java.lang.Integer, ? extends jet.Unit>")
        { (x: Int, y: Int) : Unit -> }

    check("jet.ExtensionFunctionImpl0<? super java.lang.Integer, ? extends jet.Unit>")
        { Int.() : Unit -> }
    check("jet.ExtensionFunctionImpl0<? super jet.Unit, ? extends java.lang.Integer>")
        { Unit.() : Int -> 42 }
    check("jet.ExtensionFunctionImpl1<? super java.lang.String, ? super java.lang.String, ? extends java.lang.Long>")
        { String.(s: String) : Long -> 42.toLong() }
    check("jet.ExtensionFunctionImpl2<? super java.lang.Integer, ? super java.lang.Integer, ? super java.lang.Integer, ? extends jet.Unit>")
        { Int.(x: Int, y: Int) : Unit -> }

    return "OK"
}
