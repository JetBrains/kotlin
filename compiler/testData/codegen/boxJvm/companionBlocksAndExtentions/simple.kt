// TARGET_BACKEND: JVM
// CHECK_BYTECODE_LISTING
// LANGUAGE: +CompanionBlocksAndExtensions


// FILE: J.java

public class J {}

// FILE: k.kt

import J

class A {
    val memberVal: String = "1"
    fun memberFun(k: String = "") = k

    companion {
        val compBlockVal: String = "3"
        fun compBlockFun(k: String = "") = k
    }

    companion object {
        val compObjVal: String = "5"
        fun compObjFun(k: String = "") = k

        @JvmStatic
        val compObjStaticVal: String = "7"
        @JvmStatic
        fun compObjStaticFun(k: String = "") = k
    }

}
companion val A.compExtVal: String = "9"
companion fun A.compExtFun(k: String = "") = k

companion val J.compExtValJ: String = "11"
companion fun J.compExtFunJ(k: String = "") = k

fun testK(): String {
    val res = A().memberVal + A().memberFun("2") + A.compBlockVal + A.compBlockFun("4") + A.compObjVal + A.compObjFun("6") + A.compObjStaticVal + A.compObjStaticFun("8") + A.compExtVal + A.compExtFun("10")
    println(res)
    if (res == "12345678910") return "O" else return "KFail: $res "
}

fun testJ(): String {
    val res = J.compExtValJ + J.compExtFunJ("12")
    if(res == "1112") return "K" else return "JFail: $res "
}


fun box(): String {
    return testK() + testJ()
}
