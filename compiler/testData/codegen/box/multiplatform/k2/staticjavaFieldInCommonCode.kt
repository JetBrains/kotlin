// TARGET_BACKEND: JVM_IR
// LANGUAGE: +MultiPlatformProjects
// FULL_JDK
// ISSUE: KT-63574

// MODULE: commonJvm
// FILE: common.kt
import java.nio.ByteOrder

fun foo(): String = ByteOrder.LITTLE_ENDIAN.toString()

// MODULE: platformJvm()()(commonJvm)
// FILE: platform.kt
import java.nio.ByteOrder

fun bar(): String = ByteOrder.LITTLE_ENDIAN.toString()

fun box(): String {
    val expected = "LITTLE_ENDIAN"
    if (foo() != expected) return "Fail 1"
    if (bar() != expected) return "Fail 2"
    return "OK"
}
