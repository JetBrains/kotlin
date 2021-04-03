// !WITH_NEW_INFERENCE

fun foo(x: String): String? = x

fun calc(x: String?, y: Int?): Int {
    // Smart cast because of x!! in receiver
    foo(x!!)?.subSequence(y!!, x.length)?.length
    // No smart cast possible
    return <!RETURN_TYPE_MISMATCH!>y<!>
}
