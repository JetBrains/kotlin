// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: FRONTEND
// MODULE: m1-common
// FILE: common.kt

expect class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>A<!>()

expect class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>B<!>()

<!CONFLICTING_OVERLOADS!>expect fun foo(test: String)<!>

<!CONFLICTING_OVERLOADS!>fun test()<!> {
    <!DEPRECATION_ERROR{JVM}!>A<!>()
    <!UNRESOLVED_REFERENCE{JVM}!>B<!>()
    <!OVERLOAD_RESOLUTION_AMBIGUITY!>foo<!>("")
}

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

@Deprecated("", level = DeprecationLevel.HIDDEN)
actual class A

actual class B @Deprecated("", level = DeprecationLevel.HIDDEN) actual constructor(){}

@Deprecated("", level = DeprecationLevel.HIDDEN)
actual fun foo(test: String) {
}

fun main() {
    <!DEPRECATION_ERROR!>A<!>()
    <!UNRESOLVED_REFERENCE!>B<!>()
    foo("")
}
