// TARGET_BACKEND: JVM
// WITH_STDLIB
// DISABLE_IR_VISIBILITY_CHECKS: JVM_IR, JVM_IR_SERIALIZE

// FILE: foo.kt

@file:Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
@file:JvmPackageName("jjj")
@file:JvmName("Foooo")

fun f(): String = "O"

val g: String? get() = "K"

inline fun i(block: () -> String) = block()

// FILE: bar.kt

fun box(): String = i { f() + g }
