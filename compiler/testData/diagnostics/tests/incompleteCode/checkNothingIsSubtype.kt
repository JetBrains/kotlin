// DIAGNOSTICS: -UNUSED_EXPRESSION
package d

interface A<T>

fun <T> infer(a: A<T>) : T {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>

fun test(nothing: Nothing?) {
    val i = <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>infer<!>(<!DEBUG_INFO_CONSTANT, TYPE_MISMATCH!>nothing<!>)
}

fun sum(a : IntArray) : Int {
for (n
<!SYNTAX!>return<!><!SYNTAX!><!> "?"
<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>
