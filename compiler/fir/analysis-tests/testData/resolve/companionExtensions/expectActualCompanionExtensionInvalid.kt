// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +CompanionBlocksAndExtensions +MultiPlatformProjects
// MODULE: m1-common
// FILE: common.kt
class Foo

expect companion fun Foo.bar()
expect companion val Foo.baz: String
expect companion var Foo.qux: Int

expect fun Foo.bar2()
expect val Foo.baz2: String
expect var Foo.qux2: Int

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt
actual fun Foo.<!ACTUAL_WITHOUT_EXPECT!>bar<!>() {}
actual val Foo.<!ACTUAL_WITHOUT_EXPECT!>baz<!>: String get() = ""
actual var Foo.<!ACTUAL_WITHOUT_EXPECT!>qux<!>: Int
    get() = 0
    set(v) {}

actual companion fun Foo.<!ACTUAL_WITHOUT_EXPECT!>bar2<!>() {}
actual companion val Foo.<!ACTUAL_WITHOUT_EXPECT!>baz2<!>: String get() = ""
actual companion var Foo.<!ACTUAL_WITHOUT_EXPECT!>qux2<!>: Int = 0

/* GENERATED_FIR_TAGS: actual, classDeclaration, expect, funWithExtensionReceiver, functionDeclaration, getter,
integerLiteral, propertyDeclaration, propertyWithExtensionReceiver, stringLiteral */
