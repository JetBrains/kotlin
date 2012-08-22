package d

trait A<T>

fun infer<T>(<!UNUSED_PARAMETER!>a<!>: A<T>) : T {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>

fun test(nothing: Nothing?) {
    val <!UNUSED_VARIABLE!>i<!> = <!TYPE_INFERENCE_TYPE_CONSTRUCTOR_MISMATCH!>infer<!>(nothing)
}

fun sum(<!UNUSED_PARAMETER!>a<!> : IntArray) : Int {
    for (<!UNREACHABLE_CODE!>n<!><!SYNTAX!><!>
    <!ITERATOR_AMBIGUITY!>return <!TYPE_MISMATCH!>"?"<!><!><!SYNTAX!><!>
<!SYNTAX!><!>}
