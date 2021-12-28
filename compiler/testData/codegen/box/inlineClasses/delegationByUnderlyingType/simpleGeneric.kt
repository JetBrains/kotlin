// WITH_STDLIB
// IGNORE_LIGHT_ANALYSIS
// IGNORE_BACKEND: JVM
// LANGUAGE: +InlineClassImplementationByDelegation, +GenericInlineClassParameter

interface I {
    fun ok(): String
}

inline class IC<T: I>(val i: T): I by i

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