// TARGET_BACKEND: JVM_IR
// WITH_STDLIB
// WITH_REFLECT
// FULL_JDK

annotation class Ann1(val value: UByte = 41u)
annotation class Ann2(val value: UShort = 42u)
annotation class Ann3(val value: UInt = 43u)
annotation class Ann4(val value: ULong = 44u)

@Ann1
@Ann2
@Ann3
@Ann4
class A

fun box(): String {
    val default1 = A::class.java.getAnnotation(Ann1::class.java).value
    val default2 = A::class.java.getAnnotation(Ann2::class.java).value
    val default3 = A::class.java.getAnnotation(Ann3::class.java).value
    val default4 = A::class.java.getAnnotation(Ann4::class.java).value

    return if (default1 == 41u.toUByte() &&
        default2 == 42u.toUShort() &&
        default3 == 43u &&
        default4 == 44u.toULong()
    ) "OK"
    else "FAIL"
}
