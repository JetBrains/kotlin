// LANGUAGE: +MultiPlatformProjects

// MODULE: common

interface A {
    open fun foo() {}
}

expect interface <!NO_ACTUAL_FOR_EXPECT!>B<!>

class C : A, <!SUPERTYPE_APPEARS_TWICE{JVM}!>B<!> {}

// MODULE: jvm()()(common)

actual typealias B = A
