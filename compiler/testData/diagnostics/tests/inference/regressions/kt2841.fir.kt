package a

interface Closeable {}
class C : Closeable {}

public inline fun <T: Closeable, R> T.use1(block: (T)-> R) : R {
    return block(this)
}

fun main() {
    C().<!CANNOT_INFER_PARAMETER_TYPE!>use1<!> {
        w ->  // ERROR here
        <!UNRESOLVED_REFERENCE!>x<!>
    }
}
