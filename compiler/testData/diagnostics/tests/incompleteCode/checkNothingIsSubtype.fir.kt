// !WITH_NEW_INFERENCE
// !DIAGNOSTICS: -UNUSED_EXPRESSION
package d

interface A<T>

fun <T> infer(a: A<T>) : T {}

fun test(nothing: Nothing?) {
    val i = <!INAPPLICABLE_CANDIDATE!>infer<!>(nothing)
}

fun sum(a : IntArray) : Int {
<!UNRESOLVED_REFERENCE, UNRESOLVED_REFERENCE, UNRESOLVED_REFERENCE!>for (n
<!SYNTAX!>return<!><!SYNTAX!><!> "?"<!>
}