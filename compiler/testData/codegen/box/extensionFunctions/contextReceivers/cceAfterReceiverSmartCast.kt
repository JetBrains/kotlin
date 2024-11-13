// LANGUAGE: +ContextReceivers
// TARGET_BACKEND: JVM_IR
// ISSUE: KT-73065

interface A {
    val C.check: Int get() = 42
}

open class B
open class C : B()

context(A)
fun B.foo(c: C) {
    val f = c.check
    if (this is C) {
        check
    }
}

fun box(): String {
    val a = object : A {}
    with(a) {
        val c = C()
        c.foo(c)
    }
    return "OK"
}