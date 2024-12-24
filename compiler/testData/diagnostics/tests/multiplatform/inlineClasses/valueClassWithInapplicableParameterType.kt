// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +MultiPlatformProjects
// WITH_STDLIB

// MODULE: common
// FILE: common.kt
expect class N

@<!UNRESOLVED_REFERENCE!>JvmInline<!>
value class A(val n: <!VALUE_CLASS_HAS_INAPPLICABLE_PARAMETER_TYPE{JVM}!>N<!>)

expect class U

@<!UNRESOLVED_REFERENCE!>JvmInline<!>
value class B(val u: <!VALUE_CLASS_HAS_INAPPLICABLE_PARAMETER_TYPE{JVM}!>U<!>)

// MODULE: jvm()()(common)
// FILE: jvm.kt
<!ACTUAL_TYPE_ALIAS_TO_NOTHING!>actual typealias N = Nothing<!>

actual typealias U = Unit
