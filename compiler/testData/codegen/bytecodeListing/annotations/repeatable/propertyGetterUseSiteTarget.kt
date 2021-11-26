// !LANGUAGE: +RepeatableAnnotations
// TARGET_BACKEND: JVM_IR
// WITH_STDLIB
// FULL_JDK
// JVM_TARGET: 1.8

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
    // "i" is not detected because of KT-48141.
    @A("h") @get:A("i") get() = 0
