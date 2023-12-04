// LANGUAGE: +MultiPlatformProjects
// IGNORE_BACKEND_K1: JVM, JVM_IR, JS, JS_IR, JS_IR_ES6, WASM
// DUMP_IR

// MODULE: common
// FILE: common.kt

open class A {
    internal open fun foo1() = "A::foo1"
    internal open fun foo2() = "A::foo2"
    internal open fun foo3() = "A::foo3"
}

expect open class B() : A {
    internal override fun foo1(): String
    internal override fun foo2(): String
}

open class C : B() {
    internal override fun foo1() = "C::foo1"
}


// MODULE: main()()(common)
// FILE: main.kt

actual open class B actual constructor() : A() {
    internal actual override fun foo1() = "B::foo1"
    internal actual override fun foo2() = "B::foo2"
}


fun box() : String {
    val a: A = A()
    val b: A = B()
    val c: A = C()
    if (a.foo1() != "A::foo1") return "Fail A.1"
    if (a.foo2() != "A::foo2") return "Fail A.2"
    if (a.foo3() != "A::foo3") return "Fail A.3"
    if (b.foo1() != "B::foo1") return "Fail B.1"
    if (b.foo2() != "B::foo2") return "Fail B.2"
    if (b.foo3() != "A::foo3") return "Fail B.3"
    if (c.foo1() != "C::foo1") return "Fail C.1"
    if (c.foo2() != "B::foo2") return "Fail C.2"
    if (c.foo3() != "A::foo3") return "Fail C.3"
    return "OK"
}
