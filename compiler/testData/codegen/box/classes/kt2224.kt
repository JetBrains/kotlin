// IGNORE_BACKEND_FIR: JVM_IR
interface A {
    fun foo(): Int
}

class B1 : A {
    override fun foo() = 10
}

class B2(val z: Int) : A {
    override fun foo() = z
}



fun f1(b: B1): Int {
    val o = object : A by b { }
    return o.foo()
}

fun f2(b: B2): Int {
    val o = object : A by B2(b.z) { }
    return o.foo()
}

fun f3(b: B2, mult: Int): Int {
    val o = object : A by B2(mult * b.z) { }
    return o.foo()
}

fun f4(b: B1, x: Int, y: Int, z: Int): Int {
    val o = object : A by b {
        fun bar() = x + y + z
    }
    return o.foo()
}


fun box(): String {
    if (f1(B1()) != 10) return "fail #1"
    if (f2(B2(239)) != 239) return "fail #2"
    if (f3(B2(239), 2) != 239*2) return "fail #3"
    if (f4(B1(), 1, 2, 3) != 10) return "fail #4"
    return "OK"
}
