class Inline() {

    inline fun foo1Int(s : (l: Int) -> Int, param: Int) : Int {
        return s(param)
    }

    inline fun foo1Double(param: Double, s : (l: Double) -> Double) : Double {
        return s(param)
    }

    inline fun foo2Param(param1: Double, s : (i: Int, l: Double) -> Double, param2: Int) : Double {
        return s(param2, param1)
    }
}

