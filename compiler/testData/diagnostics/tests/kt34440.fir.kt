// ISSUE: KT-34440

class BufferUtil {
    fun isDirect(cond: Boolean): Boolean =
        when (cond) {
            else -> throw Exception("${buf.<!SYNTAX!><!>}")
        }
    private class BufferInfo(private val type: Class<*>)
}
