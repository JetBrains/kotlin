// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_EXPRESSION -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE

class Out<out T : Any>(result: T?)

fun main() {
    val a = Out(null)

    <!DEBUG_INFO_EXPRESSION_TYPE("Out<kotlin.Nothing>")!>a<!>

    var b: Out<Int>? = null
    b = a
}