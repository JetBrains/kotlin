// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: BACKEND
// MODULE: m1-common
// FILE: common.kt

<!CONFLICTING_OVERLOADS!>expect <!EXPECTED_TAILREC_FUNCTION, EXPECTED_TAILREC_FUNCTION{JVM}!>tailrec<!> fun foo(p: Int): Int<!>
<!CONFLICTING_OVERLOADS!>expect fun bar(p: Int): Int<!>

expect <!WRONG_MODIFIER_TARGET, WRONG_MODIFIER_TARGET{JVM}!>tailrec<!> val <!REDECLARATION!>notReport<!>: String

expect class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>A<!> {
    <!EXPECTED_TAILREC_FUNCTION, EXPECTED_TAILREC_FUNCTION{JVM}!>tailrec<!> fun foo(p: Int): Int
    fun bar(p: Int): Int
}

// MODULE: m1-jvm()()(m1-common)
// FILE: jvm.kt
actual tailrec fun foo(p: Int): Int = foo(p)
actual tailrec fun bar(p: Int): Int = bar(p)

actual val notReport: String = "123"

actual class A {
    actual tailrec fun foo(p: Int): Int = foo(p)
    actual tailrec fun bar(p: Int): Int = bar(p)
}
