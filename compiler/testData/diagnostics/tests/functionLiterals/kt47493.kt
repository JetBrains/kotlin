// IGNORE_LEAKED_INTERNAL_TYPES: KT-54568
fun test1() {
    try {
        { <!CANNOT_INFER_PARAMETER_TYPE!>toDouble<!> ->
        }
    } catch (e: Exception) {

    }
}

fun test2() {
    try {

    } catch (e: Exception) {
        { <!CANNOT_INFER_PARAMETER_TYPE!>toDouble<!> ->
        }
    }
}

fun box(): String {
    test1()
    test2()
    return "OK"
}
