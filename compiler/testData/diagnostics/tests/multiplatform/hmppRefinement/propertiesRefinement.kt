// LANGUAGE: +ExpectRefinement
// WITH_STDLIB
// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: BACKEND
// MODULE: common
expect internal val <!REDECLARATION{JVM;JVM;JVM}!>foo<!>: Int

// MODULE: intermediate1()()(common)
<!OPT_IN_WITHOUT_ARGUMENTS, OPT_IN_WITHOUT_ARGUMENTS{JVM}!>@OptIn(<!ANNOTATION_ARGUMENT_MUST_BE_CONST, ANNOTATION_ARGUMENT_MUST_BE_CONST{JVM}!><!UNRESOLVED_REFERENCE, UNRESOLVED_REFERENCE{JVM}!>ExperimentalMultiplatform<!>::class<!>)<!>
<!WRONG_ANNOTATION_TARGET{JVM}!>@kotlin.<!UNRESOLVED_REFERENCE, UNRESOLVED_REFERENCE{JVM}!>experimental<!>.<!DEBUG_INFO_MISSING_UNRESOLVED, DEBUG_INFO_MISSING_UNRESOLVED{JVM}!>ExpectRefinement<!><!>
expect internal val <!REDECLARATION, REDECLARATION{JVM;JVM}!>foo<!>: Int

// MODULE: intermediate2()()(intermediate1)
<!OPT_IN_WITHOUT_ARGUMENTS!>@OptIn(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!><!UNRESOLVED_REFERENCE!>ExperimentalMultiplatform<!>::class<!>)<!>
<!WRONG_ANNOTATION_TARGET{JVM}!>@kotlin.<!UNRESOLVED_REFERENCE!>experimental<!>.<!DEBUG_INFO_MISSING_UNRESOLVED!>ExpectRefinement<!><!>
expect public val <!REDECLARATION, REDECLARATION{JVM}!>foo<!>: Int

// MODULE: main()()(intermediate2)
actual public val <!AMBIGUOUS_EXPECTS!>foo<!>: Int = 1

/* GENERATED_FIR_TAGS: actual, classReference, expect, integerLiteral, propertyDeclaration */
