// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +CustomEqualsInInlineClasses
// TARGET_BACKEND: JVM_IR

OPTIONAL_JVM_INLINE_ANNOTATION
value class A(val x: Int) {
    fun equals(other: A) = x % 5 == other.x % 5
}

OPTIONAL_JVM_INLINE_ANNOTATION
value class B(val x: A)

fun box() = if (B(A(0)) == B(A(5))) "OK" else "Fail"

// CHECK_BYTECODE_TEXT
// 0 INVOKESTATIC B.box-impl
// 0 INVOKESTATIC A.box-impl