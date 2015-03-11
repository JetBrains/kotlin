class Foo private(val param: String = "OK") {
    default object {
        val s = Foo()
    }
}

fun box(): String {
    Foo.s.param
    return "OK"
}
