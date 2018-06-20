// IGNORE_BACKEND: JS_IR
class A {
    enum class E {
        OK
    }
}

fun box() = A.E.OK.toString()
