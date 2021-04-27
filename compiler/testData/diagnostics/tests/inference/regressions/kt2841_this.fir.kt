// !WITH_NEW_INFERENCE

package a

interface Closeable {
    fun close() {}
}

class C : Closeable

public inline fun <T: Closeable, R> T.use(block: T.()-> R) : R {
    return this.block()
}

fun test() {
    C().use {
        this.close()
        <!ARGUMENT_TYPE_MISMATCH, UNRESOLVED_REFERENCE!>x<!>
    }
}
