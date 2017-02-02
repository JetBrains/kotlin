open class C {
    open fun f() = "<fun:C>"
}

class C1: C() {
    override fun f() = super<C>.f() + "<fun:C1>"
}

open class C2: C() {
}

class C3: C2() {
    override fun f() = super<C2>.f() + "<fun:C3>"
}

fun main(args: Array<String>) {
    println(C1().f())
    println(C3().f())
}