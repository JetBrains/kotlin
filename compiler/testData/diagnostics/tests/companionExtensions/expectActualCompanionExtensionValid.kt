// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +CompanionBlocksAndExtensions +MultiPlatformProjects
// MODULE: m1-common
// FILE: common.kt
class Foo

expect companion fun Foo.bar()
expect companion val Foo.baz: String
expect companion var Foo.qux: Int

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt
actual companion fun Foo.bar() {}
actual companion val Foo.baz: String get() = ""
actual companion var Foo.qux: Int = 0

/* GENERATED_FIR_TAGS: actual, classDeclaration, expect, funWithExtensionReceiver, functionDeclaration, getter,
integerLiteral, propertyDeclaration, propertyWithExtensionReceiver, stringLiteral */
