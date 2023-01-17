// IGNORE_BACKEND: NATIVE
// IGNORE_BACKEND: JS_IR, JS_IR_ES6
// MODULE: lib
// FILE: 2.kt
class Host {
    abstract class B : A()

    abstract class A {
        private val value = "OK"
        fun f() = value
    }
}

// FILE: 3.kt
abstract class C : Host.B()

// MODULE: main(lib)
// FILE: 1.kt
class D : C()

fun box(): String = D().f()

// FILE: 2.kt
class Host {
    abstract class B : A()

    abstract class A {
        private val value = "OK"
        fun f() = value
    }
}
