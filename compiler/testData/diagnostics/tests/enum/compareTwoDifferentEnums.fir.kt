// !LANGUAGE: -ProhibitComparisonOfIncompatibleEnums
// FILE: JavaEnumA.java

public enum JavaEnumA {}

// FILE: JavaEnumB.java

public enum JavaEnumB {}

// FILE: test.kt

enum class KotlinEnumA
enum class KotlinEnumB

fun jj(a: JavaEnumA, b: JavaEnumB) = <!EQUALITY_NOT_APPLICABLE_WARNING!>a == b<!>
fun jk(a: JavaEnumA, b: KotlinEnumB) = <!EQUALITY_NOT_APPLICABLE_WARNING!>a == b<!>
fun kk(a: KotlinEnumA, b: KotlinEnumB) = <!EQUALITY_NOT_APPLICABLE_WARNING!>a == b<!>
