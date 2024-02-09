
package a

interface Closeable {}
class C : Closeable {}

fun <T: Closeable, R> T.foo(block: (T)-> R) = block

fun <T: Closeable, R> T.foo(block: (T, T)-> R) = block

fun main() {
    C().<!CANNOT_INFER_PARAMETER_TYPE!>foo<!> <!CANNOT_INFER_PARAMETER_TYPE!>{ // no ambiguity here
        www ->
        <!UNRESOLVED_REFERENCE!>xs<!>
    }<!>
}
