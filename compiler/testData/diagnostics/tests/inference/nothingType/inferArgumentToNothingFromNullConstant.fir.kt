// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_EXPRESSION -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE

class Out<out T : Any>(result: T?)

fun main() {
    val a = Out(null)

    a

    var b: Out<Int>? = null
    b = a
}