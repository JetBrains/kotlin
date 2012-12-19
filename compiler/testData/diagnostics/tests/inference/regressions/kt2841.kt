package a

trait Closeable {}
class C : Closeable {}

public inline fun <T: Closeable, R> T.use1(block: (T)-> R) : R {
    return block(this)
}

fun main(args: Array<String>) {
    C().use1 {
        w ->  // ERROR here
        <!UNRESOLVED_REFERENCE!>x<!>
    }
}