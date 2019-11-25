// IGNORE_BACKEND_FIR: JVM_IR
interface M {
    var backingB : Int
    var b : Int
        get() = backingB
        set(value: Int) {
            backingB = value
        }
}

class N() : M {
    public override var backingB : Int = 0

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
