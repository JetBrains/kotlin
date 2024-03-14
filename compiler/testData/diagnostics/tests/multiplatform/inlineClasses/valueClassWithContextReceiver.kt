// LANGUAGE: +MultiPlatformProjects, +ContextReceivers
// WITH_STDLIB
// IGNORE_NON_REVERSED_RESOLVE
// IGNORE_REVERSED_RESOLVE

// MODULE: common
// FILE: common.kt
expect value class <!NO_ACTUAL_FOR_EXPECT!>A<!>(val s: String)

class Ctx

// MODULE: jvm()()(common)
// FILE: jvm.kt
<!VALUE_CLASS_CANNOT_HAVE_CONTEXT_RECEIVERS!>context(Ctx)<!>
@JvmInline
actual value class A(val s: String)

// MODULE: js()()(common)
// TARGET_PLATFORM: JS
// FILE: js.kt
<!VALUE_CLASS_CANNOT_HAVE_CONTEXT_RECEIVERS!>context(Ctx)<!>
actual value class A(val s: String)
