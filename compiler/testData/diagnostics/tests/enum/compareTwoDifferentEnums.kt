// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// FILE: JavaEnumA.java

public enum JavaEnumA {}

// FILE: JavaEnumB.java

public enum JavaEnumB {}

// FILE: test.kt

enum class KotlinEnumA
enum class KotlinEnumB

fun jj(a: JavaEnumA, b: JavaEnumB) = <!INCOMPATIBLE_ENUM_COMPARISON_ERROR!>a == b<!>
fun jk(a: JavaEnumA, b: KotlinEnumB) = <!INCOMPATIBLE_ENUM_COMPARISON_ERROR!>a == b<!>
fun kk(a: KotlinEnumA, b: KotlinEnumB) = <!INCOMPATIBLE_ENUM_COMPARISON_ERROR!>a == b<!>

fun jj2(a: JavaEnumA, b: JavaEnumB) = <!INCOMPATIBLE_ENUM_COMPARISON_ERROR!>a === b<!>
fun jk2(a: JavaEnumA, b: KotlinEnumB) = <!INCOMPATIBLE_ENUM_COMPARISON_ERROR!>a === b<!>
fun kk2(a: KotlinEnumA, b: KotlinEnumB) = <!INCOMPATIBLE_ENUM_COMPARISON_ERROR!>a === b<!>
