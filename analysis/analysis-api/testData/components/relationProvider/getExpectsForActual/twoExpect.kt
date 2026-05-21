// LANGUAGE: +MultiPlatformProjects
// callable: sample/some

// MODULE: common
// TARGET_PLATFORM: Common
// FILE: Common.kt

package sample

expect fun some(): Int

// MODULE: jvm()()(common)
// TARGET_PLATFORM: JVM

// Intentional misuse: 'expect' declaration with a body in the implementation module
// COMPILATION_ERRORS

// FILE: Jvm.kt

package sample

<expr>expect fun some(): Int = 42</expr>
