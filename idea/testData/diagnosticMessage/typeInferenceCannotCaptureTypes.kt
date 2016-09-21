// !DIAGNOSTICS_NUMBER: 1
// !DIAGNOSTICS: TYPE_INFERENCE_CANNOT_CAPTURE_TYPES

fun <R : Any> bar(a: Array<R>): Array<R?> =  null!!

fun test1(a: Array<in Int>) {
    val r: Array<in Int?> = bar(a)
}
