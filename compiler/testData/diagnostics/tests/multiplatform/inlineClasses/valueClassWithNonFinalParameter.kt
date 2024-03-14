// LANGUAGE: +MultiPlatformProjects
// WITH_STDLIB

// MODULE: common
// FILE: common.kt
expect value class <!NO_ACTUAL_FOR_EXPECT!>A<!>(val s: String)

// MODULE: jvm()()(common)
// FILE: jvm.kt
@JvmInline
actual value class A(<!VALUE_CLASS_CONSTRUCTOR_NOT_FINAL_READ_ONLY_PARAMETER!><!NON_FINAL_MEMBER_IN_FINAL_CLASS!>open<!> val s: String<!>)

// MODULE: js()()(common)
// TARGET_PLATFORM: JS
// FILE: js.kt
actual value class A(<!VALUE_CLASS_CONSTRUCTOR_NOT_FINAL_READ_ONLY_PARAMETER!><!NON_FINAL_MEMBER_IN_FINAL_CLASS!>open<!> val s: String<!>)
