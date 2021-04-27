// !WITH_NEW_INFERENCE

package a

interface Closeable {}
class C : Closeable {}

fun <T: Closeable, R> T.foo(block: (T)-> R) = block

fun <T: Closeable, R> T.foo(block: (T, T)-> R) = block

fun main() {
    C().foo { // no ambiguity here
        www ->
        <!ARGUMENT_TYPE_MISMATCH, UNRESOLVED_REFERENCE!>xs<!>
    }
}
