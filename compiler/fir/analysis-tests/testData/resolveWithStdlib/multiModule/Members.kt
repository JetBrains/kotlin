// !LANGUAGE: +MultiPlatformProjects
// MODULE: m1-common
// FILE: common.kt

expect open class A() {
    fun foo()

    val x: Int
}

open class B : A()

// MODULE: m1-jvm()()(m1-common)
// FILE: jvm.kt

actual open class A {
    actual fun foo() {}

    fun bar() {}

    actual val x = 42
}

class C : B() {
    fun test() {
        foo()
        bar()
        x + x
    }
}

class D : A() {
    fun test() {
        foo()
        bar()
        x + x
    }
}
