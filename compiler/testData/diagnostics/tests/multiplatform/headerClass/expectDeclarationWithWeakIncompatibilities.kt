// !LANGUAGE: +MultiPlatformProjects
// !DIAGNOSTICS: -UNUSED_PARAMETER
// MODULE: m1-common
// FILE: common.kt

expect class Foo1
expect class Foo2

expect fun foo1(): Int
expect fun foo2(): Int
expect fun foo3<!JVM:NO_ACTUAL_FOR_EXPECT!>()<!>: Int

// MODULE: m2-jvm(m1-common)

// FILE: jvm.kt

<!ACTUAL_WITHOUT_EXPECT!>interface<!> Foo1
actual <!ACTUAL_WITHOUT_EXPECT!>interface<!> Foo2

actual fun foo1(): <!ACTUAL_WITHOUT_EXPECT!>String<!> = ""

fun <!ACTUAL_MISSING!>foo2<!>(): Int = 0
fun foo3(x: Int): String = ""
