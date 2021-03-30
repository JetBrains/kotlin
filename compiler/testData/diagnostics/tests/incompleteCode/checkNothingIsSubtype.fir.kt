// !WITH_NEW_INFERENCE
// !DIAGNOSTICS: -UNUSED_EXPRESSION
package d

interface A<T>

fun <T> infer(a: A<T>) : T {}

fun test(nothing: Nothing?) {
    val i = infer(<!ARGUMENT_TYPE_MISMATCH!>nothing<!>)
}

fun sum(a : IntArray) : Int {
<!UNRESOLVED_REFERENCE, UNRESOLVED_REFERENCE, UNRESOLVED_REFERENCE!>for (n
<!SYNTAX!>return<!><!SYNTAX!><!> "?"<!>
}
