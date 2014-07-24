inline fun Inline.calcExt(s: (Int) -> Int, p: Int) : Int {
    return s(p)
}

inline fun  Inline.calcExt2(s: Int.() -> Int, p: Int) : Int {
    return p.s()
}

class InlineX(val value : Int) {}

class Inline(val res: Int) {

    inline fun InlineX.calcInt(s: (Int, Int) -> Int) : Int {
        return s(res, this.value)
    }

    inline fun Double.calcDouble(s: (Int, Double) -> Double) : Double {
        return s(res, this)
    }

    fun doWork(l : InlineX) : Int {
        return l.calcInt({(a: Int, b: Int) -> a + b})
    }

    fun doWorkWithDouble(s : Double) : Double {
        return s.calcDouble({(a: Int, b: Double) -> a + b})
    }

}
