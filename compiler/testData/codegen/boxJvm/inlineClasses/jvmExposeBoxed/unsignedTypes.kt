// WITH_STDLIB
// TARGET_BACKEND: JVM_IR
// CHECK_BYTECODE_LISTING

// FILE: IC.kt
@file:OptIn(ExperimentalStdlibApi::class)

// 'UInt' is covered by 'uint.kt'; these are the remaining unsigned value classes.
@JvmExposeBoxed("createUByte")
fun makeUByte(): UByte = 1u

@JvmExposeBoxed("createUShort")
fun makeUShort(): UShort = 2u

@JvmExposeBoxed("createULong")
fun makeULong(): ULong = 3uL

@JvmExposeBoxed
fun concat(b: UByte, s: UShort, l: ULong): String = "$b$s$l"

// FILE: Main.java
public class Main {
    public String test() {
        return ICKt.concat(ICKt.createUByte(), ICKt.createUShort(), ICKt.createULong());
    }
}

// FILE: Box.kt
fun box(): String {
    val res = Main().test()
    if (res != "123") return "FAIL: $res"
    return "OK"
}
