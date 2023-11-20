// MODULE: m1-common()()()
// FILE: common.kt
expect class Foo {
    class Inner
    class AbsentOnactual
}

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt
class FooImpl {
    class Inner
}

actual typealias <!NO_ACTUAL_CLASS_MEMBER_FOR_EXPECTED_CLASS!>Foo<!> = FooImpl
