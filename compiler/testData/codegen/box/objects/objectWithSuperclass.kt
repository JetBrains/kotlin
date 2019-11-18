// IGNORE_BACKEND_FIR: JVM_IR
open class A {
    open fun foo(): Int {
        return 2
    }
}

object O: A() {
    override fun foo(): Int {
        val s = super<A>.foo()
        return s + 3
    }
}

fun box() : String {
  return if (O.foo() == 5) "OK" else "fail"
}
