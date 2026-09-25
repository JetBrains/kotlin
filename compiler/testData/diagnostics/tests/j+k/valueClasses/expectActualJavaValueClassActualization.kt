// RUN_PIPELINE_TILL: FIR2IR
// LANGUAGE: +FullValueClasses, +MultiPlatformProjects
// MODULE: m1-common
// FILE: common.kt

expect abstract value class AbstractValueExpect()

// MODULE: m2-jvm()()(m1-common)
// FILE: JavaAbstractVal.java
public abstract value class JavaAbstractVal {}

// FILE: jvm.kt
actual typealias AbstractValueExpect = JavaAbstractVal

/* GENERATED_FIR_TAGS: actual, classDeclaration, expect, javaType, primaryConstructor, typeAliasDeclaration, value */
