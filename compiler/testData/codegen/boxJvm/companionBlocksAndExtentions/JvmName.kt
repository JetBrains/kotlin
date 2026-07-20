// TARGET_BACKEND: JVM
// CHECK_BYTECODE_LISTING
// LANGUAGE: +CompanionBlocks +CompanionExtensions


// FILE: J.java

public class J {}

// FILE: k.kt

import J

class A {
    companion {
        @JvmOverloads
        @JvmName("compBlockFunR")
        fun compBlockFun(k: String = "0") = k
    }
}

@JvmOverloads
@JvmName("compExtFunR")
companion fun A.compExtFun(k: String = "1") = k

@JvmOverloads
@JvmName("compExtFunJR")
companion fun J.compExtFunJ(k: String = "2") = k

fun testK(): String {
    val res = A::class.java.getMethod("compBlockFunR", String::class.java).invoke(null, "4") as String +
            A::class.java.getMethod("compBlockFunR").invoke(null) as String +
            Class.forName("KKt").getMethod("compExtFunR", String::class.java).invoke(null, "3") as String +
            Class.forName("KKt").getMethod("compExtFunR").invoke(null) as String
    if (res == "4031") return "O" else return "KFail: $res "
}

fun testJ(): String {
    val res = Class.forName("KKt").getMethod("compExtFunJR").invoke(null) as String +
            Class.forName("KKt").getMethod("compExtFunJR", String::class.java).invoke(null, "12") as String
    if(res == "212") return "K" else return "JFail: $res "
}


fun box(): String {
    return testK() + testJ()
}
