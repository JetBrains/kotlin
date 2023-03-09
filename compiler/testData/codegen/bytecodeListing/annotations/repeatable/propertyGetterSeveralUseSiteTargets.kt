// !LANGUAGE: +RepeatableAnnotations
// TARGET_BACKEND: JVM_IR
// WITH_STDLIB
// FULL_JDK
// JVM_TARGET: 1.8

// IGNORE_BACKEND_K1: JVM_IR
// ^ "c", "d" is not detected because of KT-48141.

// IGNORE_BACKEND_K2: JVM_IR
// FIR status: ["c", "d", "a", "b"] because of KT-57219 K2: incorrect relative order of normal and use-site-targeted annotations on property getter in the resulting bytecode

@Repeatable
annotation class A(val v: String)

@get:A("a") @get:A("b")
val ab: Int
    @get:A("c") @get:A("d") get() = 0
