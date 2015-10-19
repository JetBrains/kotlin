fun trans(n: Int, f: () -> Boolean) = if (f()) n else null

fun foo() {
    var i: Int? = 5    
    if (i != null) {
        fun can(): Boolean {
            i = null
            return true
        }
        i<!UNSAFE_CALL!>.<!>hashCode()
        trans(<!TYPE_MISMATCH!>i<!>, ::can)
        i<!UNSAFE_CALL!>.<!>hashCode()
    }
}
