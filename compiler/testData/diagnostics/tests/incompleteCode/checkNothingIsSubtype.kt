// !DIAGNOSTICS: -UNUSED_EXPRESSION
package d

interface A<T>

fun <T> infer(<!UNUSED_PARAMETER!>a<!>: A<T>) : T {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>

fun test(nothing: Nothing?) {
    val <!UNUSED_VARIABLE!>i<!> = <!TYPE_INFERENCE_INCORPORATION_ERROR!>infer<!>(<!TYPE_MISMATCH, DEBUG_INFO_CONSTANT!>nothing<!>)
}

fun sum(<!UNUSED_PARAMETER!>a<!> : IntArray) : Int {
for (n
<!SYNTAX!>return<!><!SYNTAX!><!> "?"
<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>