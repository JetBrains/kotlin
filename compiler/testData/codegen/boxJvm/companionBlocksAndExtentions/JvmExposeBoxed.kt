// TARGET_BACKEND: JVM
// CHECK_BYTECODE_LISTING
// LANGUAGE: +CompanionBlocks +CompanionExtensions


// FILE: J.java

public class J {}

// FILE: k.kt
@file:OptIn(ExperimentalStdlibApi::class)

import J

class K {
    companion {
        @get:JvmExposeBoxed("getRenamed")
        val compBlockValR: UInt = 4u

        @get:JvmExposeBoxed
        val compBlockVal: UInt = 3u
        @JvmExposeBoxed
        fun compBlockFun(k: UInt = 4u) = k
    }

}
@get:JvmExposeBoxed("KextValBoxed")
companion val K.compExtVal: UInt = 5u
@JvmExposeBoxed()
companion fun K.compExtFun(k: UInt = 6u) = k

@get:JvmExposeBoxed("JextValBoxed")
companion val J.compExtValJ: UInt = 7u
@JvmExposeBoxed
companion fun J.compExtFunJ(k: UInt = 8u) = k

fun testK(): String {
    val kKt = Class.forName("KKt")
    val res = K::class.java.getMethod("getCompBlockVal").invoke(null) as UInt +
            K::class.java.getMethod("getRenamed").invoke(null) as UInt +
            K::class.java.getMethod("compBlockFun", UInt::class.java).invoke(null, 3u) as UInt +
            kKt.getMethod("KextValBoxed").invoke(null) as UInt +
            kKt.getMethod("compExtFun", UInt::class.java).invoke(null, 5u) as UInt
    if (res == 20u) return "O" else return "KFail: $res "

}

fun testJ(): String {
    val kKt = Class.forName("KKt")
    val res =
            kKt.getMethod("JextValBoxed").invoke(null) as UInt +
            kKt.getMethod("compExtFunJ", UInt::class.java).invoke(null, 12u) as UInt
    if(res == 19u) return "K" else return "JFail: $res "
}

fun box(): String {
    return testK() + testJ()
}
