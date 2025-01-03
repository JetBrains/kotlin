// RUN_PIPELINE_TILL: BACKEND
// MODULE: m1-common
// FILE: common.kt

package test

<!CONFLICTING_OVERLOADS!>expect fun foo(): String<!>

<!CONFLICTING_OVERLOADS!>fun g(f: () -> String): String<!> = f()

<!CONFLICTING_OVERLOADS!>fun test()<!> {
    <!OVERLOAD_RESOLUTION_AMBIGUITY!>g<!>(::<!DEBUG_INFO_MISSING_UNRESOLVED, DEPRECATION{JVM}!>foo<!>)
}

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

package test

@Deprecated("To check that ::foo is resolved to actual fun foo when compiling common+jvm")
actual fun foo(): String = ""
