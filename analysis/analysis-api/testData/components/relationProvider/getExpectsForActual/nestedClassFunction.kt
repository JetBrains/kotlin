// LANGUAGE: +MultiPlatformProjects
// callable: sample/Outer.Inner.foo

// MODULE: common
// TARGET_PLATFORM: Common
// FILE: Common.kt

package sample

expect class Outer {
    class Inner {
        fun foo(): Int
    }
}

// MODULE: jvm()()(common)
// TARGET_PLATFORM: JVM
// FILE: Jvm.kt

package sample

actual class Outer {
    actual class Inner {
        <expr>actual fun foo(): Int = 42</expr>
    }
}
