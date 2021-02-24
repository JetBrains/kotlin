// !LANGUAGE: +MultiPlatformProjects
// !DIAGNOSTICS: -UNUSED_PARAMETER
// MODULE: m1-common
// FILE: common.kt

expect class Foo1
expect class Foo2

expect fun foo2(): Int

expect val s: String

expect open class Foo3

// MODULE: m2-jvm(m1-common)

// FILE: jvm.kt

interface Foo1
actual interface Foo2

actual var s: String = "value"

fun foo2(): Int = 0

<!REDECLARATION!>actual class Foo3<!>

<!REDECLARATION!>class Foo3<!>
