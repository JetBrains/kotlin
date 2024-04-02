// TARGET_BACKEND: JVM
// ^ KT-67114

// MODULE: a
// FILE: a.kt

open class A {
    @PublishedApi
    open internal fun foo(): String = "OK"

    fun bar(): String = foo()
}

// MODULE: b(a)
// FILE: b.kt

class B : A() {
    internal fun foo(): String = "Fail"
}

fun box(): String = B().bar()
