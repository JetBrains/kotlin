// !LANGUAGE: +NewInference

open class A<T1, T2> {
    open inner class A1(val a1: T1)
    open inner class A2(val a2: T2)

    open fun f1(arg: T1) = arg
    open fun f2(arg: T2) = arg
}

open class B<T> : A<T, Int>() {
    open inner class B1(b1: T) : A1(b1)
    open inner class B2(b2: Int) : A2(b2)

    fun variableToKnownParameter(p: T): Int =
        p as? Int ?: 0

    inner class B3(b3: T) : A2(variableToKnownParameter(b3))

    override fun f1(arg: T) = arg
    override fun f2(arg: Int) = arg
}

class C : B<String>() {
    inner class C1(c1: String): B1(c1)
    inner class C2 : B2(15)

    override fun f1(arg: String) = arg
    override fun f2(arg: Int) = arg
}
