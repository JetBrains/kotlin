// IGNORE_BACKEND: JVM_IR
// FILE: 1.kt

class InlineDouble(val res : Double) {

    inline fun foo(s : () -> Double) : Double {
        val f = "fooStart"
        val z = s()
        return z
    }

    inline fun foo11(s : (l: Double) -> Double) : Double {
        return s(11.0)
    }

    inline fun fooRes(s : (l: Double) -> Double) : Double {
        val z = s(res)
        return z
    }

    inline fun fooRes2(s : (l: Double, t: Double) -> Double) : Double {
        val f = "fooRes2Start"
        val z = s(1.0, 11.0)
        return z
    }
}

// FILE: 2.kt

fun test0Param(): Double {
    val inlineX = InlineDouble(10.0)
    return inlineX.foo({ -> 1.0})
}

fun test1Param(): Double {
    val inlineX = InlineDouble(10.0)
    return inlineX.foo11({ z: Double -> z})
}

fun test1ParamCaptured(): Double {
    val s = 100.0
    val inlineX = InlineDouble(10.0)
    return inlineX.foo11({ z: Double -> s})
}

fun test1ParamMissed() : Double {
    val inlineX = InlineDouble(10.0)
    return inlineX.foo11({ z: Double -> 111.0})
}

fun test1ParamFromCallContext() : Double {
    val inlineX = InlineDouble(1000.0)
    return inlineX.fooRes({ z: Double -> z})
}

fun test2Params() : Double {
    val inlineX = InlineDouble(1000.0)
    return inlineX.fooRes2({ y: Double, z: Double -> 2.0 * y + 3.0 * z})
}

fun test2ParamsWithCaptured() : Double {
    val inlineX = InlineDouble(1000.0)
    val s = 9.0
    var t = 1.0
    return inlineX.fooRes2({ y: Double, z: Double -> 2.0 * s + t})
}

fun box(): String {
    if (test0Param() != 1.0) return "test0Param"
    if (test1Param() != 11.0) return "test1Param()"
    if (test1ParamCaptured() != 100.0) return "testtest1ParamCaptured()"
    if (test1ParamMissed() != 111.0) return "test1ParamMissed()"
    if (test1ParamFromCallContext() != 1000.0) return "test1ParamFromCallContext()"
    if (test2Params() != 35.0) return "test2Params()"
    if (test2ParamsWithCaptured() != 19.0) return "test2ParamsWithCaptured()"
    return "OK"
}
