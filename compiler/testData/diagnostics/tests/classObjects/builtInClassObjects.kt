// !DIAGNOSTICS: -UNUSED_VARIABLE, -UNUSED_PARAMETER

fun box(): String {
    try {
        // Objects
        val i = Int
        val d = Double
        val f = Float
        val l = Long
        val sh = Short
        val b = Byte
        val st = String

        test(Int)
        test(Double)
        test(Float)
        test(Long)
        test(Short)
        test(Byte)
        test(String)

        // Common Double
        Double.POSITIVE_INFINITY
        Double.NEGATIVE_INFINITY
        Double.NaN

        // Common Float
        Float.POSITIVE_INFINITY
        Float.NEGATIVE_INFINITY
        Float.NaN
    }
    catch (e: Throwable) {
        return "Error: \n" + e
    }

    return "OK"
}

fun test(a: Any) {}


