// IGNORE_BACKEND_FIR: JVM_IR
open class M() {
    open var b: Int = 0
}

class N() : M() {
    val a : Int
        get() {
            super.b = super.b + 1
            return super.b + 1
        }
    override var b: Int = a + 1

    val superb : Int
        get() = super.b
}

fun box(): String {
    val n = N()
    n.a
    n.b
    n.superb
    if (n.b == 3 && n.a == 4 && n.superb == 3) return "OK";
    return "fail";
}
