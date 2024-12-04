// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// SAM_CONVERSIONS: INDY
// WITH_STDLIB
// FULL_JDK

fun hello() { println("Hello, world!") }

val test = Runnable(::hello)

// 1 INVOKEDYNAMIC
// 1 class FunctionRefToJavaInterfaceKt
