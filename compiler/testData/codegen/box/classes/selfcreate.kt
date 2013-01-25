class B () {}

open class A(val b : B) {
    fun a() = object: A(b) {}
}

fun box() : String {
    A(B()).a()
    return "OK"
}
