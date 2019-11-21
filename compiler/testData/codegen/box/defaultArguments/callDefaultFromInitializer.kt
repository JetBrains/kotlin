fun foo(x: Int = 42) = x

class C {
    val s: String
    init {
        val x = foo()
        if (x == 42)
            s = "OK"
        else
            s = "fail"
    }
}

fun box(): String {
    return C().s
}
