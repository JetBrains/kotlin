// WITH_STDLIB
class A {
    enum class E {
        OK
    }
}

fun box() = A.E.OK.toString()
