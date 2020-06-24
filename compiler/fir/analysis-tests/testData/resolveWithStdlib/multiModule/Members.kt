// !LANGUAGE: +MultiPlatformProjects
// MODULE: m1-common
// FILE: common.kt

expect open class A() {
    fun foo()
}

open class B : A()

// MODULE: m1-jvm(m1-common)
// FILE: jvm.kt

actual open class A {
    actual fun foo() {}

    fun bar() {}
}

class C : B() {
    fun test() {
        foo()
        bar()
    }
}

class D : A() {
    fun test() {
        foo()
        bar()
    }
}
