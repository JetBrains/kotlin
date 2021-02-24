fun box(): String {
    var a: Int
    a = 12
    fun f() {
        foo(a)
    }

    return "OK"
}

fun foo(l: Int) {}