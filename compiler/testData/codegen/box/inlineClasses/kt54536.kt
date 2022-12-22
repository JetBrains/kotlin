// WITH_STDLIB
// LANGUAGE: +ValueClasses, +CustomEqualsInValueClasses
// TARGET_BACKEND: JVM_IR

@JvmInline
@AllowTypedEquals
value class A(val x: Int) {
    @TypedEquals
    fun equals(other: A) = x % 5 == other.x % 5
}

@JvmInline
@OptIn(AllowTypedEquals::class)
value class B(val x: A)

@OptIn(AllowTypedEquals::class)
fun box() = if (B(A(0)) == B(A(5))) "OK" else "Fail"

// CHECK_BYTECODE_TEXT
// 0 INVOKESTATIC B.box-impl
// 0 INVOKESTATIC A.box-impl