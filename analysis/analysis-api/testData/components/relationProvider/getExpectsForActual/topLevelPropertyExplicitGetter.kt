// LANGUAGE: +MultiPlatformProjects
// getter: callable: sample/name

// MODULE: common
// TARGET_PLATFORM: Common
// FILE: Common.kt

package sample

@Target(AnnotationTarget.PROPERTY_GETTER)
annotation class Hint

expect val name: String
    @Hint get

// MODULE: jvm()()(common)
// TARGET_PLATFORM: JVM
// FILE: Jvm.kt

package sample

actual val name: String
    <expr>@Hint get() = "Alice"</expr>
