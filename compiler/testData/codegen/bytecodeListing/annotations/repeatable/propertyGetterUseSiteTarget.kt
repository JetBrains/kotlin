// LANGUAGE: +RepeatableAnnotations
// TARGET_BACKEND: JVM_IR
// JVM_ABI_K1_K2_DIFF: KT-69075
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
val gh: Int
    @A("h") get() = 0

@set:A("i")
var ij: Int
    get() = 0
    @A("j") set(value) {}
