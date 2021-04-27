// !WITH_NEW_INFERENCE

package a

interface Closeable {
    fun close() {}
}

class C : Closeable

public inline fun <T: Closeable, R> T.use(block: (t: T)-> R) : R {
    return block(this)
}

fun test() {
    C().use {
        it.close()
        <!ARGUMENT_TYPE_MISMATCH, UNRESOLVED_REFERENCE!>x<!>
    }
}
