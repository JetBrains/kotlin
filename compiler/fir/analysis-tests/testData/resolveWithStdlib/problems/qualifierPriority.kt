class AHolder(val a: Int)

class F {
    object foo { // (1)
        val a: Int = 42
    }
    companion object { // (2)
        val foo: AHolder = AHolder(52)
    }
}
class FF {
    object foo {} // (1)
    companion object { // (2)
        val foo: AHolder = AHolder(52)
    }
}

fun main() {
    F.foo.a // (1) everywhere
    F.foo // (2) in old FE, (1) in FIR
    // Why companion?
    with(F.foo) {
        a // (2) in old FE, (1) in FIR
    }
    FF.foo.<!UNRESOLVED_REFERENCE!>a<!> // (1) everywhere
    // Why not companion
}