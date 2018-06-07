// IGNORE_BACKEND: JS_IR
class A {
    companion object {}
    enum class E {
        OK
    }
}

fun box() = A.E.OK.toString()
