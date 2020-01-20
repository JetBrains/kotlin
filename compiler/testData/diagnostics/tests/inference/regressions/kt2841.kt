// !WITH_NEW_INFERENCE

package a

interface Closeable {}
class C : Closeable {}

public inline fun <T: Closeable, R> T.use1(block: (T)-> R) : R {
    return block(this)
}

fun main() {
    C().<!NI;NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>use1<!> {
        <!UNUSED_ANONYMOUS_PARAMETER!>w<!> ->  // ERROR here
        <!UNRESOLVED_REFERENCE!>x<!>
    }
}
