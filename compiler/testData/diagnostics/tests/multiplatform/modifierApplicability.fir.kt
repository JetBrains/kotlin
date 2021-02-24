// !LANGUAGE: +MultiPlatformProjects
// MODULE: m1-common
// FILE: common.kt

expect typealias Foo = String

class Outer expect constructor() {
    expect class Nested

    expect init {}

    <!NON_ABSTRACT_FUNCTION_WITH_NO_BODY!>expect fun foo()<!>
    expect val bar: Int
}

fun foo() {
    expect fun localFun()
    expect var x = 42
    expect class Bar
}

// MODULE: m2-jvm
// FILE: jvm.kt

class Outer actual constructor() {
    actual class Nested

    actual init {}
}

fun foo() {
    actual fun localFun() {}
    actual var x = 42
    actual class Bar
}
