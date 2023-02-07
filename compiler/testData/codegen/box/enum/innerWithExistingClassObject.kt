// WITH_STDLIB
class A {
    companion object {}
    enum class E {
        OK
    }
}

fun box() = A.E.OK.toString()
