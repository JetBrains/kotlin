fun trans(n: Int, f: () -> Boolean) = if (f()) n else null

fun foo() {
    var i: Int? = 5    
    if (i != null) {
        class Changing {
            fun bar() {
                i = null
            }
        }
        i.hashCode()
        Changing().bar()
        i.hashCode()
    }
}
