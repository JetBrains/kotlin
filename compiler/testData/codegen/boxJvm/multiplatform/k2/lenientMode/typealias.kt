// LANGUAGE: +MultiPlatformProjects
// TARGET_BACKEND: JVM
// LENIENT_MODE
// MODULE: common
// FILE: common.kt
package pkg

expect class Foo

// MODULE: jvm()()(common)
// FILE: jvm.kt
package pkg

class C
actual typealias Foo = C

fun box(): String {
    Foo()
    return "OK"
}
