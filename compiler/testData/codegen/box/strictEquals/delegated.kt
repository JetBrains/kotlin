// LANGUAGE: +StrictEquals
// DUMP_IR

interface I {
    override fun equals(@EqualityBound(I::class) other: Any?): Boolean
}

class C : I {
    override fun equals(@EqualityBound(I::class) other: Any?): Boolean = true
}

class D : I by C()

fun box(): String {
    if (D() != D()) return "Fail#1"
    if (D() != C()) return "Fail#2"
    return "OK"
}
