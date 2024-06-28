// FIR_IDENTICAL
// LANGUAGE: -ProhibitComparisonOfIncompatibleEnums
// FILE: JavaEnumA.java

public enum JavaEnumA {}

// FILE: JavaEnumB.java

public enum JavaEnumB {}

// FILE: test.kt

enum class KotlinEnumA
enum class KotlinEnumB

fun jj(a: JavaEnumA, b: JavaEnumB) = <!INCOMPATIBLE_ENUM_COMPARISON!>a == b<!>
fun jk(a: JavaEnumA, b: KotlinEnumB) = <!INCOMPATIBLE_ENUM_COMPARISON!>a == b<!>
fun kk(a: KotlinEnumA, b: KotlinEnumB) = <!INCOMPATIBLE_ENUM_COMPARISON!>a == b<!>

fun jj2(a: JavaEnumA, b: JavaEnumB) = <!INCOMPATIBLE_ENUM_COMPARISON!>a === b<!>
fun jk2(a: JavaEnumA, b: KotlinEnumB) = <!INCOMPATIBLE_ENUM_COMPARISON!>a === b<!>
fun kk2(a: KotlinEnumA, b: KotlinEnumB) = <!INCOMPATIBLE_ENUM_COMPARISON!>a === b<!>
