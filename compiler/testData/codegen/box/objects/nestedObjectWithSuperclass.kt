// IGNORE_BACKEND_FIR: JVM_IR
open class A (val s: Int) {
    open fun foo(): Int {
        return s
    }
}

object Outer: A(1) {
    object O: A(2) {
        override fun foo(): Int {
            val s = super<A>.foo()
            return s + 3
        }
    }
}

fun box() : String {
    return if (Outer.O.foo() == 5) "OK" else "fail"
}