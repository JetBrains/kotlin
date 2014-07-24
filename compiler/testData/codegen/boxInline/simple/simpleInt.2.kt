class Inline(val res : Int) {

    inline fun foo(s : () -> Int) : Int {
        val f = "fooStart"
        val z = s()
        return z
    }

    inline fun foo11(s : (l: Int) -> Int) : Int {
        return s(11)
    }

    inline fun fooRes(s : (l: Int) -> Int) : Int {
        val z = s(res)
        return z
    }

    inline fun fooRes2(s : (l: Int, t: Int) -> Int) : Int {
        val f = "fooRes2Start"
        val z = s(1, 11)
        return z
    }
}

