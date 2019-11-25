// IGNORE_BACKEND_FIR: JVM_IR
package foo

fun box(): String {
    try {
        testCompanionObjectAccess()
        testInCall()
        testDoubleConstants()
        testFloatConstants()
        testLocalFun()
        testTopLevelFun()
        testVarTopField()
    }
    catch (e: Throwable) {
        return "Error: \n" + e
    }

    return "OK"
}

fun testCompanionObjectAccess() {
    val i = Int
    val d = Double
    val f = Float
    val l = Long
    val sh = Short
    val b = Byte
    val ch = Char
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
    test(Char)
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

fun testLocalFun() {
    fun Int.Companion.LocalFun() : String = "LocalFun"
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
var Int.Companion.TopField : Int
    get() = _field
    set(value) { _field = value };

fun Int.Companion.TopFun() : String = "TopFun"

fun <T> myAssertEquals(a: T, b: T) {
    if (a != b) throw Exception("$a != $b")
}


