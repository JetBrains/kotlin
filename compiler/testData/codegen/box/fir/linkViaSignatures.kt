// WITH_STDLIB
// TARGET_BACKEND: JVM_IR
// LINK_VIA_SIGNATURES_K1

class Class
interface Interface
sealed class Sealed<T>
enum class E { ENTRY }

fun function(s: String): Array<Int> {
    fun Boolean.local() {}
    return arrayOf(s.length)
}
typealias S = String
var property: S? = "OK"

fun box(): String {
    val c = Class()
    val o = object : Interface {}
    if (property != "OK") return property!!
    if (E.ENTRY.ordinal != 0) return E.ENTRY.ordinal.toString()
    val result = function("AlphaBeta")
    if (result.size != 1) return result.size.toString()
    if (result[0] != 9) return result[0].toString()
    return "OK"
}
