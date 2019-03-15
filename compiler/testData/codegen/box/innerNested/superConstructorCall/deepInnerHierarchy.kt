open class A(val s: String) {
    open inner class B(s: String): A(s)

    open inner class C(s: String, additional: Double): B(s)

    open inner class D(other: Int, another: Long, s: String) : C(s, another.toDouble())

    open inner class E : D(0, 42L, "OK")

    inner class F : E()
}

fun box(): String = A("Fail").F().s
