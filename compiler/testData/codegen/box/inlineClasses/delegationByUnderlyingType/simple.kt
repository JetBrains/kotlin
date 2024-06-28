// WITH_STDLIB
// IGNORE_BACKEND: JVM
// LANGUAGE: +InlineClassImplementationByDelegation

interface I {
    fun ok(): String
}

inline class IC(val i: I): I by i

fun box(): String {
    val i = object : I {
        override fun ok(): String = "OK"
    }
    var res = IC(i).ok()
    if (res != "OK") return "FAIL: $res"
    val ic: I = IC(i)
    res = ic.ok()
    return res
}
