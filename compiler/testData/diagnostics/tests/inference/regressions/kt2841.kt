package a

interface Closeable {}
class C : Closeable {}

public inline fun <T: Closeable, R> T.use1(block: (T)-> R) : R {
    return block(this)
}

fun main(args: Array<String>) {
    C().use1 {
        <!UNUSED_PARAMETER!>w<!> ->  // ERROR here
        <!UNRESOLVED_REFERENCE!>x<!>
    }
}
