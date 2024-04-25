// WITH_STDLIB
// DIAGNOSTICS: -UNUSED_PARAMETER

class A {
    @kotlin.jvm.JvmOverloads <!CONFLICTING_JVM_DECLARATIONS!>fun foo(s: String = "") {
    }<!>

    <!CONFLICTING_JVM_DECLARATIONS!>fun foo() {
    }<!>
}
