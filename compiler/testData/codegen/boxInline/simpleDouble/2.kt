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