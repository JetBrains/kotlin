
package a

interface Closeable {
    fun close() {}
}

class C : Closeable

public inline fun <T: Closeable, R> T.use(block: (t: T)-> R) : R {
    return block(this)
}

fun test() {
    C().<!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>use<!> {
        it.close()
        <!UNRESOLVED_REFERENCE!>x<!>
    }
}
