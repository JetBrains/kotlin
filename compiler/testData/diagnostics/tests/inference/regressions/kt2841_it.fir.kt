package a

interface Closeable {
    fun close() {}
}

class C : Closeable

public inline fun <T: Closeable, R> T.use(block: (t: T)-> R) : R {
    return block(this)
}

fun test() {
    C().<!CANNOT_INFER_PARAMETER_TYPE!>use<!> {
        it.close()
        <!UNRESOLVED_REFERENCE!>x<!>
    }
}
