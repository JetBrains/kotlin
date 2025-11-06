// LANGUAGE: +MultiPlatformProjects
// TARGET_BACKEND: JVM
// WITH_STDLIB

// MODULE: common
// FILE: common.kt

@file:<!OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE{METADATA}!>kotlin.jvm.JvmMultifileClass<!>
@file:<!OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE{METADATA}!>kotlin.jvm.JvmName<!>("Test")
package test

expect class Foo {
    val value: String
}

// MODULE: jvm()()(common)
// FILE: jvm.kt

@file:JvmMultifileClass
@file:JvmName("Test")
package test

actual class Foo(actual val value: String)

fun box(): String {
    return Foo("OK").value
}
