// IGNORE_BACKEND: JVM_IR
open class A {
    fun foo() = "OK"
}

fun box() = object : A() {
    fun bar() = super<A>.foo()
}.bar()
