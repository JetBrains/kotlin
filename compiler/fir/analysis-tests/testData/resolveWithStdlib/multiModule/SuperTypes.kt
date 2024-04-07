// !LANGUAGE: +MultiPlatformProjects
// MODULE: m1-common
// FILE: common.kt

expect open class A() {
    fun foo()
}

open class B : A()

// MODULE: m1-jvm()()(m1-common)
// FILE: jvm.kt

abstract class X {
    fun bar() {}
}

interface Y {
    fun baz() {}
}

actual open class A : X(), Y {
    actual fun foo() {}
}

class C : B() {
    fun test() {
        foo()
        bar()
        baz()
    }
}

class D : A() {
    fun test() {
        foo()
        bar()
        baz()
    }
}
