// FIR_IDENTICAL
// ISSUE: KT-34440

class BufferUtil {
    fun isDirect(cond: Boolean): Boolean =
        when (cond) {
            else -> throw Exception("${<!UNRESOLVED_REFERENCE!>buf<!>.<!SYNTAX!><!>}")
        }
    private class BufferInfo(private val type: Class<*>)
}
