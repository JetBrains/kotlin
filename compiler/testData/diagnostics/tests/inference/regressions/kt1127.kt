// !WITH_NEW_INFERENCE
//KT-1127 Wrong type computed for Arrays.asList()

package d

fun <T> asList(<!UNUSED_PARAMETER!>t<!>: T) : List<T>? {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>

fun main(args : Array<String>) {
    val <!UNUSED_VARIABLE!>list<!> : List<String> = <!TYPE_INFERENCE_EXPECTED_TYPE_MISMATCH!>asList("")<!>
}