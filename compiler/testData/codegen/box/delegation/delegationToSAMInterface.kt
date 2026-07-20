fun interface F {
    fun f()
}

object F1 : F by { }

fun box(): String {
    F1.f()
    return "OK"
}
