// WITH_STDLIB
// TARGET_BACKEND: JVM_IR

// FILE: J.java
public class J {
    public static RegularClass createRegularClass() {
        return new RegularClass();
    }
    public static RegularClass2 createRegularClass2() {
        return new RegularClass2();
    }
}

// FILE: Box.kt
@file:OptIn(ExperimentalStdlibApi::class)

@JvmInline
value class IntWrapper(val i: Int = 0)

@JvmExposeBoxed
class RegularClass(val property: IntWrapper = IntWrapper(2))

@JvmExposeBoxed
class RegularClass2(val property: IntWrapper = IntWrapper(2), val s: String = "OK")

fun box(): String {
    var kotlin = RegularClass().property
    var java = J.createRegularClass().property
    if (kotlin != java) return "FAIL 1: $kotlin != $java"
    kotlin = RegularClass2().property
    java = J.createRegularClass2().property
    if (kotlin != java) return "FAIL 2: $kotlin != $java"
    return "OK"
}