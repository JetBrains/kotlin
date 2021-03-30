// !WITH_NEW_INFERENCE
// !CHECK_TYPE

package d

import checkSubtype

fun <T: Any> joinT(x: Int, vararg a: T): T? {
    return null
}

fun <T: Any> joinT(x: Comparable<*>, y: T): T? {
    return null
}

fun test() {
    val x2 = <!NONE_APPLICABLE!>joinT<!>(Unit, "2")
    checkSubtype<String?>(<!ARGUMENT_TYPE_MISMATCH!>x2<!>)
}
