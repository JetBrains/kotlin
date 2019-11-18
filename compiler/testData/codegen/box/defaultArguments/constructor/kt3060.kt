// IGNORE_BACKEND_FIR: JVM_IR
class Foo private constructor(val param: String = "OK") {
    companion object {
        val s = Foo()
    }
}

fun box(): String {
    Foo.s.param
    return "OK"
}
