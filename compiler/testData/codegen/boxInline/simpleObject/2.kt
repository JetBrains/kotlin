class InlineString(val res : String) {

    inline fun foo(s : () -> String) : String {
        val f = "fooStart"
        val z = s()
        return z
    }

    inline fun foo11(s : (l: String) -> String) : String {
        return s("11")
    }

    inline fun fooRes(s : (l: String) -> String) : String {
        val z =  s(res)
        return z
    }

    inline fun fooRes2(s : (l: String, t: String) -> String) : String {
        val f = "fooRes2Start"
        val z = s("1", "11")
        return z
    }
}

