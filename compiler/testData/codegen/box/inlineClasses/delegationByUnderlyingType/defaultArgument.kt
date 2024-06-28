// WITH_STDLIB
// IGNORE_BACKEND: JVM
// LANGUAGE: +InlineClassImplementationByDelegation
// JVM_ABI_K1_K2_DIFF: KT-65534

interface I {
    fun o(k: String = "K"): String = "O$k"
}

inline class IC(val i: I): I by i

fun box(): String {
    val i = object : I {}
    val ic1 = IC(i)
    var res = ic1.o()
    if (res != "OK") return "FAIL 1: $res"
    res = ic1.o("KK")
    if (res != "OKK") return "FAIL 2: $res"

    val ic2: I = IC(i)
    res = ic2.o()
    if (res != "OK") return "FAIL 3: $res"
    res = ic2.o("KK")
    if (res != "OKK") return "FAIL 4: $res"

    return "OK"
}
