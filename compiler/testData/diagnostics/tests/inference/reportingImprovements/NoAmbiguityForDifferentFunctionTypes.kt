// !WITH_NEW_INFERENCE

package a

interface Closeable {}
class C : Closeable {}

fun <T: Closeable, R> T.foo(block: (T)-> R) = block

fun <T: Closeable, R> T.foo(block: (T, T)-> R) = block

fun main() {
    C().<!NI;NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>foo<!> { // no ambiguity here
        <!UNUSED_ANONYMOUS_PARAMETER!>www<!> ->
        <!UNRESOLVED_REFERENCE!>xs<!>
    }
}

