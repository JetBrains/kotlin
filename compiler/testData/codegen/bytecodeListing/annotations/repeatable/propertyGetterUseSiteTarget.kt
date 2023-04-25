// !LANGUAGE: +RepeatableAnnotations
// TARGET_BACKEND: JVM_IR
// WITH_STDLIB
// FULL_JDK
// JVM_TARGET: 1.8

// IGNORE_BACKEND_K2: JVM_IR
// FIR status: KT-57219 K2: incorrect relative order of normal and use-site-targeted annotations on property getter in the resulting bytecode

@Repeatable
annotation class A(val v: String)

@get:A("a") @get:A("b")
val ab = 0

@get:A("c") @get:A("d")
val cd: Int
    get() = 0

val ef: Int
    @A("e") @A("f") get() = 0

@get:A("g")
val ghi: Int
    @A("h") get() = 0
