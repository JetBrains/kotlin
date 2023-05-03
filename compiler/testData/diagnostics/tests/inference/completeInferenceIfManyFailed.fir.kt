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
    val x2 = joinT(<!ARGUMENT_TYPE_MISMATCH!>Unit<!>, "2")
    checkSubtype<String?>(x2)
}
