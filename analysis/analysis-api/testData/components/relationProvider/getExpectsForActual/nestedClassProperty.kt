// LANGUAGE: +MultiPlatformProjects
// callable: sample/Outer.Inner.name

// MODULE: common
// TARGET_PLATFORM: Common
// FILE: Common.kt

package sample

expect class Outer {
    class Inner {
        val name: String
    }
}

// MODULE: jvm()()(common)
// TARGET_PLATFORM: JVM
// FILE: Jvm.kt

package sample

actual class Outer {
    actual class Inner {
        <expr>actual val name: String
            get() = "JVM"</expr>
    }
}
