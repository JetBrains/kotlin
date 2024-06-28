// SKIP_KT_DUMP
// TARGET_BACKEND: JVM
// MUTE_SIGNATURE_COMPARISON_K2: JVM_IR

// FILE: Java1.java
public class Java1 extends A { }

// FILE: Java2.java
public class Java2 { }

// FILE: 1.kt
open class A {
    open fun A.foo(a: Int) {}
    open var A.a: Int
        get() = 1
        set(value) {}

    open fun Java2.bar(a: Int) {}
    open var Java2.b: String
        get() = "java2"
        set(value) {}
}

class B : Java1() {  //Kotlin ← Java ← Kotlin
    fun test() {
        A().a
        Java2().b
        A().foo(1)
        Java2().bar(1)
    }
}

class C: Java1() {
    override var A.a: Int
        get() = 2
        set(value) {}
    override var Java2.b: String
        get() = "C.java2"
        set(value) {}

    override fun A.foo(a: Int) { }
    override fun Java2.bar(a: Int) { }

    fun test() {
        A().a
        Java2().b
        A().foo(1)
        Java2().bar(1)
    }
}