// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: FIR2IR
// MODULE: m1-common
// FILE: common.kt

open class A {}
<!EXPECT_ACTUAL_IR_INCOMPATIBILITY{JVM}!>expect<!> class B : A

expect open class A2() {}
<!EXPECT_ACTUAL_IR_INCOMPATIBILITY{JVM}!>expect<!> open class B2 : A2 {}

expect open class A3

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

actual typealias <!EXPECT_ACTUAL_INCOMPATIBLE_SUPERTYPES!>B<!> = A

actual typealias A2 = B2
actual open class <!EXPECT_ACTUAL_INCOMPATIBLE_SUPERTYPES!>B2<!> {}

actual typealias A3 = Any

/* GENERATED_FIR_TAGS: actual, classDeclaration, expect, primaryConstructor, typeAliasDeclaration */
