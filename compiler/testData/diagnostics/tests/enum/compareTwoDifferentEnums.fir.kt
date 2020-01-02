// !LANGUAGE: -ProhibitComparisonOfIncompatibleEnums
// FILE: JavaEnumA.java

public enum JavaEnumA {}

// FILE: JavaEnumB.java

public enum JavaEnumB {}

// FILE: test.kt

enum class KotlinEnumA
enum class KotlinEnumB

fun jj(a: JavaEnumA, b: JavaEnumB) = a == b
fun jk(a: JavaEnumA, b: KotlinEnumB) = a == b
fun kk(a: KotlinEnumA, b: KotlinEnumB) = a == b
