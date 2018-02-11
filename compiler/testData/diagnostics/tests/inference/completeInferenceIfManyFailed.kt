// !WITH_NEW_INFERENCE
// !CHECK_TYPE

package d

fun <T: Any> joinT(<!UNUSED_PARAMETER!>x<!>: Int, vararg <!UNUSED_PARAMETER!>a<!>: T): T? {
    return null
}

fun <T: Any> joinT(<!UNUSED_PARAMETER!>x<!>: Comparable<*>, <!UNUSED_PARAMETER!>y<!>: T): T? {
    return null
}

fun test() {
    val x2 = <!OI;TYPE_INFERENCE_PARAMETER_CONSTRAINT_ERROR!>joinT<!>(<!TYPE_MISMATCH!>Unit<!>, "2")
    checkSubtype<String?>(x2)
}