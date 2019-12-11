package a

interface Closeable {
    fun close() {}
}

class C : Closeable

public inline fun <T: Closeable, R> use(t: T, block: T.(T)-> R) : R {
    return t.<!UNRESOLVED_REFERENCE!>block<!>(t)
}

fun test() {
    use(C()) {
        this.close()
        it.close()
        <!UNRESOLVED_REFERENCE!>xx<!>
    }
}