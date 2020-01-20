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
    C().<!NI;NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>use<!> {
        this.close()
        <!UNRESOLVED_REFERENCE!>x<!>
    }
}