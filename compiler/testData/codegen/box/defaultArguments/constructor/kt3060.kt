class Foo private(val param: String = "OK") {
    class object {
        val s = Foo()
    }
}

fun box(): String {
    Foo.s.param
    return "OK"
}
