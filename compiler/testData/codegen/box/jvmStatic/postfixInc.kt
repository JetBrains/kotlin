// TARGET_BACKEND: JVM
// WITH_STDLIB
// JVM_ABI_K1_K2_DIFF: KT-63984

object A {

    @JvmStatic var a: Int = 1

    var b: Int = 1
        @JvmStatic get

    var c: Int = 1
        @JvmStatic set

}

var holder = ""
fun getA(): A {
    holder += "getA()"
    return A
}


fun box(): String {

    var p = A.a++
    if (p != 1 || A.a != 2) return "fail 1"

    p = A.b++
    if (p != 1 || A.b != 2) return "fail 2"

    p = A.c++
    if (p != 1 || A.c != 2) return "fail 3"


    p = getA().a++
    if (p != 2 || A.a != 3 || holder != "getA()") return "fail 4: $holder"
    holder = ""

    p = getA().b++
    if (p != 2 || A.b != 3 || holder != "getA()") return "fail 5: $holder"
    holder = ""

    p = getA().c++
    if (p != 2 || A.c != 3 || holder != "getA()") return "fail 6: $holder"
    holder = ""

    return "OK"
}
