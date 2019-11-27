// IGNORE_BACKEND_FIR: JVM_IR
open class A {
    fun foo() = "OK"
}

fun box() = object : A() {
    fun bar() = super<A>.foo()
}.bar()
