// LANGUAGE: +MultiPlatformProjects, +ContextReceivers
// WITH_STDLIB
// IGNORE_NON_REVERSED_RESOLVE
// IGNORE_REVERSED_RESOLVE

// MODULE: common
// FILE: common.kt
<!EXPECT_ACTUAL_INCOMPATIBILITY{JVM}, EXPECT_ACTUAL_INCOMPATIBILITY{JS}!>expect value class A<!EXPECT_ACTUAL_MISMATCH{JVM}, EXPECT_ACTUAL_MISMATCH{JS}!>(val s: String)<!><!>

class Ctx

// MODULE: jvm()()(common)
// FILE: jvm.kt
<!VALUE_CLASS_CANNOT_HAVE_CONTEXT_RECEIVERS!>context(Ctx)
@JvmInline
actual value class A(val s: String)<!>

// MODULE: js()()(common)
// TARGET_PLATFORM: JS
// FILE: js.kt
<!VALUE_CLASS_CANNOT_HAVE_CONTEXT_RECEIVERS!>context(Ctx)
actual value class A(val s: String)<!>
