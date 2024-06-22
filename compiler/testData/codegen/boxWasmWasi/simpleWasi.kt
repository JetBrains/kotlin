interface A {
    fun f()
}

interface B {
    fun g()
}

interface C {
    fun h()
}


class C1 : A, C {
    override fun f() = println("A")
    override fun h() = println("A")
}

class C2 : B, C {
    override fun g() = println("BB")
    override fun h() = println("BB")
}

class C3 : A, B {
    override fun f() = println("CC")
    override fun g() = println("CC")
}

fun box(): String {

    val c1 = C1()
    val c2 = C2()
    val c3 = C3()

    return "OK"
}