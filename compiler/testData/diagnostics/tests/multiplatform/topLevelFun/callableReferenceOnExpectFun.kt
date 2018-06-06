// !WITH_NEW_INFERENCE
// !LANGUAGE: +MultiPlatformProjects
// MODULE: m1-common
// FILE: common.kt

package test

expect fun foo(): String

fun g(f: () -> String): String = f()

fun test() {
    g(::<!OI;JVM:DEPRECATION!>foo<!>)
}

// MODULE: m2-jvm(m1-common)
// FILE: jvm.kt

package test

@Deprecated("To check that ::foo is resolved to actual fun foo when compiling common+jvm")
actual fun foo(): String = ""
