// LL_FIR_DIVERGENCE
// No JVM EXPECT_ACTUAL_IR_INCOMPATIBILITY in LL
// LL_FIR_DIVERGENCE
// LATEST_LV_DIFFERENCE
// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +MultiPlatformProjects
// MODULE: m1-common
expect class A
expect error class B
expect error class C
expect error class D
expect error class E
// MODULE: m1-jvm()()(m1-common)
actual error class <!EXPECT_ACTUAL_INCOMPATIBLE_CLASS_MODIFIERS!>A<!>
actual error class B
actual class <!EXPECT_ACTUAL_INCOMPATIBLE_CLASS_MODIFIERS, EXPECT_ACTUAL_INCOMPATIBLE_SUPERTYPES!>C<!>
error class Foo
<!ACTUAL_TYPEALIAS_WITH_EXPECT_RICH_ERROR!>actual typealias D = Foo<!>
<!ACTUAL_TYPEALIAS_WITH_EXPECT_RICH_ERROR!>actual typealias <!EXPECT_ACTUAL_INCOMPATIBLE_CLASS_MODIFIERS, EXPECT_ACTUAL_INCOMPATIBLE_SUPERTYPES!>E<!> = String<!>

/* GENERATED_FIR_TAGS: actual, classDeclaration, expect */
