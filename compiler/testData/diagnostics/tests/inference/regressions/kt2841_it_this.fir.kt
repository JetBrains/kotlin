
package a

interface Closeable {
    fun close() {}
}

class C : Closeable

public inline fun <T: Closeable, R> use(t: T, block: T.(T)-> R) : R {
    return t.block(t)
}

fun test() {
    <!CANNOT_INFER_PARAMETER_TYPE!>use<!>(C()) <!CANNOT_INFER_PARAMETER_TYPE!>{
        this.close()
        it.close()
        <!UNRESOLVED_REFERENCE!>xx<!>
    }<!>
}
