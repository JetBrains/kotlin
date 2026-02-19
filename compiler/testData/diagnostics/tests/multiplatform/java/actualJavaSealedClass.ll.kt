// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: FIR2IR
// ISSUE: KT-67581

// MODULE: common
// FILE: common.kt
expect abstract class Memory

// MODULE: jvm()()(common)
// FILE: kotlin.kt

actual typealias <!EXPECT_ACTUAL_INCOMPATIBLE_MODALITY!>Memory<!> = J

// FILE: J.java
public abstract sealed class J permits J1 {
}

// FILE: J1.java
public final class J1 extends J {
}

/* GENERATED_FIR_TAGS: actual, classDeclaration, expect, javaType, typeAliasDeclaration */
