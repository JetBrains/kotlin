// !WITH_NEW_INFERENCE
// !CHECK_TYPE

package a

import checkSubtype

fun <T> emptyList(): List<T> = throw Exception()

fun <T> foo(f: T.() -> Unit, l: List<T>): T = throw Exception("$f$l")

fun test() {
    val q = foo(fun Int.() {}, emptyList()) //type inference no information for parameter error
    checkSubtype<Int>(q)

    foo({}, emptyList())
}
