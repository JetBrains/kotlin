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
        fun compBlockFun(k: String = "0") = k
    }
}

@JvmOverloads
companion fun A.compExtFun(k: String = "1") = k

@JvmOverloads
companion fun J.compExtFunJ(k: String = "2") = k

fun testK(): String { 
    val res = A::class.java.getMethod("compBlockFun", String::class.java).invoke(null, "4") as String +
            Class.forName("KKt").getMethod("compExtFun", String::class.java).invoke(null, "3") as String +
            A::class.java.getMethod("compBlockFun").invoke(null) as String +
            Class.forName("KKt").getMethod("compExtFun").invoke(null) as String
    if (res == "4301") return "O" else return "KFail: $res "
}

fun testJ(): String {
    val res = Class.forName("KKt").getMethod("compExtFunJ").invoke(null) as String +
            Class.forName("KKt").getMethod("compExtFunJ", String::class.java).invoke(null, "12") as String
    if(res == "212") return "K" else return "JFail: $res "
}


fun box(): String {
    return testK() + testJ()
}
