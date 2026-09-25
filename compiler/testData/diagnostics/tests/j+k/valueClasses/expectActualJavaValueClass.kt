// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +FullValueClasses, +MultiPlatformProjects
// JVM_TARGET: 28
// ENABLE_JVM_PREVIEW
// JDK_KIND: FULL_JDK_21
// MODULE: m1-common
// FILE: common.kt

expect class IdentityExpect

expect abstract value class AbstractValueExpect()

expect class IdentityExpectForJdk

// MODULE: m2-jvm()()(m1-common)
// FILE: JavaVal.java
public value class JavaVal {}

// FILE: JavaAbstractVal.java
public abstract value class JavaAbstractVal {}

// FILE: jvm.kt
actual typealias IdentityExpect = JavaVal

actual typealias <!EXPECT_ACTUAL_INCOMPATIBLE_CLASS_MODIFIERS!>AbstractValueExpect<!> = JavaAbstractVal

actual typealias IdentityExpectForJdk = java.time.LocalDate

/* GENERATED_FIR_TAGS: actual, classDeclaration, expect, javaType, primaryConstructor, typeAliasDeclaration, value */
