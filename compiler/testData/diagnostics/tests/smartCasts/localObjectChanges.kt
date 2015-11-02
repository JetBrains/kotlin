fun trans(n: Int, f: () -> Boolean) = if (f()) n else null

fun foo() {
    var i: Int? = 5    
    if (i != null) {
        // Write is AFTER this place
        <!DEBUG_INFO_SMARTCAST!>i<!>.hashCode()
        object {
            fun bar() {
                i = null
            }
        }.bar()
        <!SMARTCAST_IMPOSSIBLE!>i<!>.hashCode()
    }
}
