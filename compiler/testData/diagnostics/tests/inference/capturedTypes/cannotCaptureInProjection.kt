// !WITH_NEW_INFERENCE
// !DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER
// !CHECK_TYPE

fun <T: Any> bar(a: Array<T>): Array<T?> =  null!!

fun test1(a: Array<in Int>) {
    val r: Array<in Int?> = <!OI;TYPE_INFERENCE_CANNOT_CAPTURE_TYPES!>bar<!>(<!NI;TYPE_MISMATCH!>a<!>)
    <!OI;TYPE_INFERENCE_CANNOT_CAPTURE_TYPES!>bar<!>(<!NI;TYPE_MISMATCH!>a<!>)
}

fun <T: Any> foo(l: Array<T>): Array<Array<T?>> = null!!

fun test2(a: Array<in Int>) {
    val r: Array<out Array<in Int?>> = <!OI;TYPE_INFERENCE_CANNOT_CAPTURE_TYPES!>foo<!>(<!NI;TYPE_MISMATCH!>a<!>)
    <!OI;TYPE_INFERENCE_CANNOT_CAPTURE_TYPES!>foo<!>(<!NI;TYPE_MISMATCH!>a<!>)
}