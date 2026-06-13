// LANGUAGE: +MultiPlatformProjects
// class: sample/Outer.Inner

// MODULE: common
// TARGET_PLATFORM: Common
// FILE: Common.kt

package sample

expect class Outer {
    class Inner
}

// MODULE: jvm()()(common)
// TARGET_PLATFORM: JVM
// FILE: Jvm.kt

package sample

actual class Outer {
    <expr>actual class Inner</expr>
}
