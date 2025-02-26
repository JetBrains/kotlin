// LANGUAGE: +ExpectRefinement
// IGNORE_FIR_DIAGNOSTICS
// WITH_STDLIB
// RUN_PIPELINE_TILL: BACKEND

// MODULE: common1
<!CONFLICTING_OVERLOADS{JVM}, CONFLICTING_OVERLOADS{JVM}!>expect fun foo(a: Int = 1)<!>

// MODULE: common2()()(common1)
<!CONFLICTING_OVERLOADS, CONFLICTING_OVERLOADS{JVM}!><!WRONG_ANNOTATION_TARGET{JVM}!>@kotlin.<!UNRESOLVED_REFERENCE!>experimental<!>.<!DEBUG_INFO_MISSING_UNRESOLVED!>ExperimentalExpectRefinement<!><!>
expect fun foo(a: Int)<!>

fun bar() {
    foo()
}

// MODULE: main()()(common2)
actual fun <!AMBIGUOUS_EXPECTS!>foo<!>(a: Int) {}
