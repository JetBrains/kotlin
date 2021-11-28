// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// SAM_CONVERSIONS: INDY
// WITH_STDLIB
// FULL_JDK

fun hello() { println("Hello, world!") }

val test = Runnable(::hello)

// JVM_TEMPLATES:
// 1 public final class FunctionRefToJavaInterfaceKt
// 1 final synthetic class FunctionRefToJavaInterfaceKt\$sam\$java_lang_Runnable\$0
// 1 final synthetic class FunctionRefToJavaInterfaceKt\$test\$1

// JVM_IR_TEMPLATES:
// 1 INVOKEDYNAMIC
// 1 class FunctionRefToJavaInterfaceKt
