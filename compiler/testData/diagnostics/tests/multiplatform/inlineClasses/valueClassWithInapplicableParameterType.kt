// LANGUAGE: +MultiPlatformProjects
// WITH_STDLIB

// MODULE: common
// FILE: common.kt
expect class <!NO_ACTUAL_FOR_EXPECT!>N<!>

@JvmInline
value class A(val n: <!VALUE_CLASS_HAS_INAPPLICABLE_PARAMETER_TYPE{JVM}!>N<!>)

expect class <!NO_ACTUAL_FOR_EXPECT!>U<!>

@JvmInline
value class B(val u: <!VALUE_CLASS_HAS_INAPPLICABLE_PARAMETER_TYPE{JVM}!>U<!>)

// MODULE: jvm()()(common)
// FILE: jvm.kt
<!ACTUAL_TYPE_ALIAS_TO_NOTHING!>actual typealias N = Nothing<!>

actual typealias U = Unit
