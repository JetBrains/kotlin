// !WITH_NEW_INFERENCE
// !DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER
// !CHECK_TYPE

fun <T: Any> bar(a: Array<T>): Array<T?> =  null!!

fun test1(a: Array<in Int>) {
    val r: Array<in Int?> = <!TYPE_INFERENCE_CANNOT_CAPTURE_TYPES{OI}!>bar<!>(<!TYPE_MISMATCH{NI}, TYPE_MISMATCH{NI}!>a<!>)
    <!TYPE_INFERENCE_CANNOT_CAPTURE_TYPES{OI}!>bar<!>(<!TYPE_MISMATCH{NI}, TYPE_MISMATCH{NI}!>a<!>)
}

fun <T: Any> foo(l: Array<T>): Array<Array<T?>> = null!!

fun test2(a: Array<in Int>) {
    val r: Array<out Array<in Int?>> = <!TYPE_INFERENCE_CANNOT_CAPTURE_TYPES{OI}!>foo<!>(<!TYPE_MISMATCH{NI}, TYPE_MISMATCH{NI}!>a<!>)
    <!TYPE_INFERENCE_CANNOT_CAPTURE_TYPES{OI}!>foo<!>(<!TYPE_MISMATCH{NI}, TYPE_MISMATCH{NI}!>a<!>)
}
