// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: BACKEND
// MODULE: m1-common
// FILE: common.kt
expect class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>Foo<!> {
    class Nested
}

<!CONFLICTING_OVERLOADS!>fun foo(p: Foo.<!UNRESOLVED_REFERENCE{JVM}!>Nested<!>)<!> {}

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt
class FooImpl {
    class Nested
}

actual typealias Foo = FooImpl

fun test() {
    foo(FooImpl.Nested())
}
