// LANGUAGE: +MultiPlatformProjects
// callable: sample/some

// MODULE: common
// TARGET_PLATFORM: Common

// Intentional misuse: no 'expect' on common side
// COMPILATION_ERRORS

// FILE: Common.kt

package sample

fun some(): Int

// MODULE: jvm()()(common)
// TARGET_PLATFORM: JVM

// Intentional misuse: 'expect' with a body in the implementation module
// COMPILATION_ERRORS

// FILE: Jvm.kt

package sample

<expr>expect fun some(): Int = 42</expr>
