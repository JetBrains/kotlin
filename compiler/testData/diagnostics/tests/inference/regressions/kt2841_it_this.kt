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
    <!NI;NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>use<!>(C()) {
        this.close()
        it.close()
        <!UNRESOLVED_REFERENCE!>xx<!>
    }
}