package d

trait A<T>

fun infer<T>(<!UNUSED_PARAMETER!>a<!>: A<T>) : T {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>

fun test(nothing: Nothing?) {
    val <!UNUSED_VARIABLE!>i<!> = <!TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>infer<!>(<!TYPE_MISMATCH!>nothing<!>)
}

fun sum(<!UNUSED_PARAMETER!>a<!> : IntArray) : Int {
for (n
<!SYNTAX!>return<!><!SYNTAX!><!> "?"
<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>