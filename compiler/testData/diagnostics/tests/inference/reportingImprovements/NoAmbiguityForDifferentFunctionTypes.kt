package a

interface Closeable {}
class C : Closeable {}

fun <T: Closeable, R> T.foo(block: (T)-> R) = block

fun <T: Closeable, R> T.foo(block: (T, T)-> R) = block

fun main(args: Array<String>) {
    C().foo { // no ambiguity here
        www ->
        <!UNRESOLVED_REFERENCE!>xs<!>
    }
}

