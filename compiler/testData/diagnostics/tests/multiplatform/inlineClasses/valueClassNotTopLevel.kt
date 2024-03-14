// LANGUAGE: +MultiPlatformProjects
// WITH_STDLIB

// MODULE: common
// FILE: common.kt
expect class <!NO_ACTUAL_FOR_EXPECT!>A<!>

// MODULE: jvm()()(common)
// FILE: jvm.kt
actual class A {
    @JvmInline
    value class B(val s: String)

    @JvmInline
    inner <!VALUE_CLASS_NOT_TOP_LEVEL!>value<!> class C(val s: String)
}

// MODULE: js()()(common)
// TARGET_PLATFORM: JS
// FILE: js.kt
actual class A {
    value class B(val s: String)

    inner <!VALUE_CLASS_NOT_TOP_LEVEL!>value<!> class C(val s: String)
}
