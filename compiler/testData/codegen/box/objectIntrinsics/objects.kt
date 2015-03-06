package foo

fun box(): String {
    try {
        testDefaultObjectAccess()
        testInCall()
        testDoubleConstants()
        testFloatConstants()
        testCallInterface()
        testLocalFun()
        testTopLevelFun()
        testVarTopField()
    }
    catch (e: Throwable) {
        return "Error: \n" + e
    }

    return "OK"
}

fun testDefaultObjectAccess() {
    val i = Int
    val d = Double
    val f = Float
    val l = Long
    val sh = Short
    val b = Byte
    val st = String
    val en = Enum
}

fun testInCall() {
    test(Int)
    test(Double)
    test(Float)
    test(Long)
    test(Short)
    test(Byte)
    test(String)
    test(Enum)
}

fun testDoubleConstants() {
    val pi = Double.POSITIVE_INFINITY
    val ni = Double.NEGATIVE_INFINITY
    val nan = Double.NaN

    myAssertEquals(pi, Double.POSITIVE_INFINITY)
    myAssertEquals(ni, Double.NEGATIVE_INFINITY)
}

fun testFloatConstants() {
    val pi = Float.POSITIVE_INFINITY
    val ni = Float.NEGATIVE_INFINITY
    val nan = Float.NaN

    myAssertEquals(pi, Float.POSITIVE_INFINITY)
    myAssertEquals(ni, Float.NEGATIVE_INFINITY)
}

fun testCallInterface() {
    fun <T> floatPointConstants(a: FloatingPointConstants<T>) {
        val pi = a.POSITIVE_INFINITY
        val ni = a.NEGATIVE_INFINITY
        val nan = a.NaN

        myAssertEquals(pi, a.POSITIVE_INFINITY)
        myAssertEquals(ni, a.NEGATIVE_INFINITY)
    }

    floatPointConstants(Double)
    floatPointConstants(Float)
}

fun testLocalFun() {
    fun Int.Default.LocalFun() : String = "LocalFun"
    myAssertEquals("LocalFun", Int.LocalFun())
}

fun testTopLevelFun() {
    myAssertEquals("TopFun", Int.TopFun())
}

fun testVarTopField() {
    myAssertEquals(0, Int.TopField)

    Int.TopField++
    myAssertEquals(1, Int.TopField)

    Int.TopField += 5
    myAssertEquals(6, Int.TopField)
}

fun test(a: Any) {}

var _field: Int = 0
var Int.Default.TopField : Int
    get() = _field
    set(value) { _field = value };

fun Int.Default.TopFun() : String = "TopFun"

fun myAssertEquals<T>(a: T, b: T) {
    if (a != b) throw Exception("$a != $b")
}


