class A {
    default object {}
    enum class E {
        OK
    }
}

fun box() = A.E.OK.toString()
