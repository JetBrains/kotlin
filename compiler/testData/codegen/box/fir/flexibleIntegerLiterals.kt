// TARGET_BACKEND: JVM
// WITH_STDLIB
// JVM_TARGET: 1.8
// IGNORE_BACKEND: ANDROID
// FULL_JDK

// This test fails on FE 1.0, but works in production compiler (see KT-49191):
// Caused by: java.lang.AssertionError: Lower bound Function<in (String..String?), out (IntegerLiteralType[Int,Long,Byte,Short]..IntegerLiteralType[Int,Long,Byte,Short]?)> of a flexible type must be a subtype of the upper bound Function<in (String..String?), out (IntegerLiteralType[Int,Long,Byte,Short]..IntegerLiteralType[Int,Long,Byte,Short]?)>?
// at org.jetbrains.kotlin.types.FlexibleTypeImpl.runAssertions(flexibleTypes.kt:105)
fun box(): String {
    val x = Comparator.comparing { x: String ->
        1
    }

    if (x.compare("O", "K") != 0) return "fail"

    return "OK"
}
