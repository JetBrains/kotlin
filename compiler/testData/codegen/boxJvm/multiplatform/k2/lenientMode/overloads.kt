// LANGUAGE: +MultiPlatformProjects
// TARGET_BACKEND: JVM_IR
// LENIENT_MODE
// IGNORE_HMPP: JVM_IR

// MODULE: common
// FILE: common.kt
package pkg

expect fun foo()
expect fun foo(s: String)

// MODULE: jvm()()(common)
// FILE: jvm.kt
package pkg

actual fun foo() {}

fun box(): String {
    foo()
    foo("")
    return "OK"
}
