// !WITH_NEW_INFERENCE
// !CHECK_TYPE

package d

fun <T: Any> joinT(x: Int, vararg a: T): T? {
    return null
}

fun <T: Any> joinT(x: Comparable<*>, y: T): T? {
    return null
}

fun test() {
    val x2 = <!NONE_APPLICABLE!>joinT<!>(Unit, "2")
    <!INAPPLICABLE_CANDIDATE!>checkSubtype<!><String?>(x2)
}
