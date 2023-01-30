// TARGET_BACKEND: JVM_IR

// FILE: classes.kt

open class A {
    class Inner
    fun foo(i: Inner): Inner = Inner()
}

class B: A()

// A and A$Inner both need an inner class attribute for the relationship. B does not.
// 2 INNERCLASS A\$Inner A Inner
