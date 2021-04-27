// !WITH_NEW_INFERENCE

package a

interface Closeable {
    fun close() {}
}

class C : Closeable

public inline fun <T: Closeable, R> use(t: T, block: T.(T)-> R) : R {
    return t.block(t)
}

fun test() {
    use(C()) {
        this.close()
        it.close()
        <!ARGUMENT_TYPE_MISMATCH, UNRESOLVED_REFERENCE!>xx<!>
    }
}
