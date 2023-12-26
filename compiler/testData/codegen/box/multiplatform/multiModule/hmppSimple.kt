// TARGET_BACKEND: JVM_IR
// !LANGUAGE: +MultiPlatformProjects
// JVM_ABI_K1_K2_DIFF: KT-63903

// MODULE: common
// TARGET_PLATFORM: Common
// FILE: common.kt

expect class A constructor() {
    fun foo(): String
}

expect class B constructor() {
    fun bar(): String
}

expect fun func(): String

expect var prop: String

fun test_1(): String {
    val a = A()
    val b = B()
    prop = "Set prop in common."
    return "${func()} $prop ${a.foo()} ${b.bar()}"
}

// MODULE: intermediate()()(common)
// TARGET_PLATFORM: Common
// FILE: intermediate.kt

actual fun func(): String = "Actual func."

actual class B actual constructor() {
    actual fun bar(): String = "Actual B."
}

expect class C constructor() {
    fun baz(): String
}

fun test_2(): String {
    val a = A()
    val b = B()
    val c = C()
    prop = "Set prop in intermediate."
    return "${func()} $prop ${a.foo()} ${b.bar()} ${c.baz()}"
}

// MODULE: jvm()()(intermediate)
// TARGET_PLATFORM: JVM
// FILE: main.kt

actual var prop: String = "!"

actual class A actual constructor() {
    actual fun foo(): String = "Actual A."
}

actual class C actual constructor() {
    actual fun baz(): String = "Actual C."
}

fun box(): String {
    val s1 = test_1()
    if (s1 != "Actual func. Set prop in common. Actual A. Actual B.") return s1
    val s2 = test_2()
    if (s2 != "Actual func. Set prop in intermediate. Actual A. Actual B. Actual C.") return s2
    return "OK"
}
