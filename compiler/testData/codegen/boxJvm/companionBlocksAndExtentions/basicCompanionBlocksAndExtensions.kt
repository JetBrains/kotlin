// TARGET_BACKEND: JVM
// CHECK_BYTECODE_LISTING
// LANGUAGE: +CompanionBlocks +CompanionExtensions


// FILE: J.java

public class J {}

// FILE: k.kt

import J

class A {
    val memberVal: String = "1"
    fun memberFun(k: String = "0") = k

    companion {
        val compBlockVal: String = "3"
        fun compBlockFun(k: String = "0") = k
    }

    companion object {
        val compObjVal: String = "5"
        fun compObjFun(k: String = "0") = k

        @JvmStatic
        val compObjStaticVal: String = "7"
        @JvmStatic
        fun compObjStaticFun(k: String = "0") = k
    }

}
companion val A.compExtVal: String = "9"
companion fun A.compExtFun(k: String = "0") = k

companion val J.compExtValJ: String = "11"
companion fun J.compExtFunJ(k: String = "0") = k

fun testK(): String {
    val a = A()
    val res = a.javaClass.getMethod("getMemberVal").invoke(a) as String +
            a.javaClass.getMethod("memberFun", String::class.java).invoke(a, "2") as String +
            A::class.java.getMethod("getCompBlockVal").invoke(null) as String +
            A::class.java.getMethod("compBlockFun", String::class.java).invoke(null, "4") as String +
            A.Companion::class.java.getMethod("getCompObjVal").invoke(A.Companion) as String +
            A.Companion::class.java.getMethod("compObjFun", String::class.java).invoke(A.Companion, "6") as String +
            A::class.java.getMethod("getCompObjStaticVal").invoke(null) as String +
            A::class.java.getMethod("compObjStaticFun", String::class.java).invoke(null, "8") as String +
            Class.forName("KKt").getMethod("getCompExtVal").invoke(null) as String +
            Class.forName("KKt").getMethod("compExtFun", String::class.java).invoke(null, "10") as String
    println(res)
    if (res == "12345678910") return "O" else return "KFail: $res "
}

fun testJ(): String {
    val res = Class.forName("KKt").getMethod("getCompExtValJ").invoke(null) as String +
            Class.forName("KKt").getMethod("compExtFunJ", String::class.java).invoke(null, "12") as String
    if(res == "1112") return "K" else return "JFail: $res "
}


fun box(): String {
    return testK() + testJ()
}
