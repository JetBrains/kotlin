open class A(val s: String)

fun box(): String {
    class B {
        val result = "OK"

        val f = object : A(result) {}.s
    }

    return B().f
}
