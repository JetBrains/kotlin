fun trans(n: Int, f: () -> Boolean) = if (f()) n else null

fun foo() {
    var i: Int? = 5    
    if (i != null) {
        class Changing {
            fun bar() {
                i = null
            }
        }
        i<!UNSAFE_CALL!>.<!>hashCode()
        Changing().bar()
        i<!UNSAFE_CALL!>.<!>hashCode()
    }
}
