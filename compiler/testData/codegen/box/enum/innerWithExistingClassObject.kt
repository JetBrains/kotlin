// KT-55828
// IGNORE_BACKEND_K2: NATIVE
class A {
    companion object {}
    enum class E {
        OK
    }
}

fun box() = A.E.OK.toString()
