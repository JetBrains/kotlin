// RUN_PIPELINE_TILL: FRONTEND
// IGNORE_FIR_DIAGNOSTICS
// LANGUAGE: +ContextParameters
// WITH_STDLIB

// MODULE: m1-common
// FILE: common.kt
expect value class Value(val <!AMBIGUOUS_ACTUALS{JVM}, REDECLARATION, REDECLARATION{JVM}!>x<!>: Int) {
    <!PROPERTY_WITH_BACKING_FIELD_INSIDE_VALUE_CLASS, PROPERTY_WITH_BACKING_FIELD_INSIDE_VALUE_CLASS{JVM}!><!CONTEXT_PARAMETERS_UNSUPPORTED, CONTEXT_PARAMETERS_UNSUPPORTED{JVM}!>context(x: <!DEBUG_INFO_MISSING_UNRESOLVED, DEBUG_INFO_MISSING_UNRESOLVED{JVM}!>String<!>)<!>
    val <!AMBIGUOUS_ACTUALS{JVM}, REDECLARATION, REDECLARATION{JVM}!>x<!>: Int<!>
}

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt
@JvmInline
actual value class Value(val <!AMBIGUOUS_EXPECTS, REDECLARATION!>x<!>: Int) {
    <!CONTEXT_PARAMETERS_UNSUPPORTED!>context(x: <!DEBUG_INFO_MISSING_UNRESOLVED!>String<!>)<!>
    val <!AMBIGUOUS_EXPECTS, REDECLARATION!>x<!>: Int get() = 1}

/* GENERATED_FIR_TAGS: actual, classDeclaration, expect, getter, integerLiteral, primaryConstructor, propertyDeclaration,
propertyDeclarationWithContext, value */
