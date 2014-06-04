class Inline() {

    inline fun foo1(closure1 : (l: Int) -> Int, param1: Int, closure2 : (l: Double) -> Double, param2: Double) : Double {
        return closure1(param1) + closure2(param2)
    }

    inline fun foo2(closure1 : (Int, Int) -> Int, param1: Int, closure2 : (Double, Int, Int) -> Double, param2: Double, param3: Int) : Double {
        return closure1(param1, param3) + closure2(param2, param1, param3)
    }
}

