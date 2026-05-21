// LANGUAGE: +MultiPlatformProjects
// callable: sample/foo

// MODULE: common1
// TARGET_PLATFORM: Common
// FILE: Common1.kt

package sample

expect fun foo()

// MODULE: common2
// TARGET_PLATFORM: Common
// FILE: Common2.kt

package sample

expect fun foo()

// MODULE: jvm()()(common1, common2)
// TARGET_PLATFORM: JVM

// "Conflicting overloads" reported by the compiler
// COMPILATION_ERRORS

// FILE: Jvm.kt

package sample

<expr>actual fun foo() {}</expr>
