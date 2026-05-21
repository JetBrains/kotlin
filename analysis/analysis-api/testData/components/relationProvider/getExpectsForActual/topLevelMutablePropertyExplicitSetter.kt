// LANGUAGE: +MultiPlatformProjects
// setter: callable: sample/counter

// MODULE: common
// TARGET_PLATFORM: Common
// FILE: Common.kt

package sample

expect var counter: Int
    private set

// MODULE: jvm()()(common)
// TARGET_PLATFORM: JVM
// FILE: Jvm.kt

package sample

actual var counter: Int = 0
    <expr>private set(value) {}</expr>
