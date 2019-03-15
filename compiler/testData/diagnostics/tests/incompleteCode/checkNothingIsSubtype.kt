// !WITH_NEW_INFERENCE
// !DIAGNOSTICS: -UNUSED_EXPRESSION
package d

interface A<T>

fun <T> infer(<!UNUSED_PARAMETER!>a<!>: A<T>) : T {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>

fun test(nothing: Nothing?) {
    <!NI;UNREACHABLE_CODE!>val <!UNUSED_VARIABLE!>i<!> =<!> <!OI;TYPE_INFERENCE_INCORPORATION_ERROR!>infer<!>(<!DEBUG_INFO_CONSTANT, TYPE_MISMATCH!>nothing<!>)
}

fun sum(<!UNUSED_PARAMETER!>a<!> : IntArray) : Int {
for (n
<!SYNTAX!>return<!><!SYNTAX!><!> "?"
<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>