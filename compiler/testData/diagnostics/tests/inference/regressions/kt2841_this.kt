package a

trait Closeable {
    fun close() {}
}

class C : Closeable

public inline fun <T: Closeable, R> T.use(block: T.()-> R) : R {
    return this.block()
}

fun test() {
    C().<!TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>use<!> {
        this.close()
        <!UNRESOLVED_REFERENCE!>x<!>
    }
}
