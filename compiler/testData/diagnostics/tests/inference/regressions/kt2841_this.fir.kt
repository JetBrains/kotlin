
package a

interface Closeable {
    fun close() {}
}

class C : Closeable

public inline fun <T: Closeable, R> T.use(block: T.()-> R) : R {
    return this.block()
}

fun test() {
    C().<!CANNOT_INFER_PARAMETER_TYPE!>use<!> <!CANNOT_INFER_PARAMETER_TYPE!>{
        this.close()
        <!UNRESOLVED_REFERENCE!>x<!>
    }<!>
}
