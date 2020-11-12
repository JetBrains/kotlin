// !WITH_NEW_INFERENCE
// !CHECK_TYPE

package a

import checkSubtype

fun <T> emptyList(): List<T> = throw Exception()

fun <T> foo(f: T.() -> Unit, l: List<T>): T = throw Exception("$f$l")

fun test() {
    val q = foo(fun Int.() {}, emptyList()) //type inference no information for parameter error
    checkSubtype<Int>(q)

    <!NI;NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER, OI;TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>foo<!>({}, <!NI;NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER, OI;TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>emptyList<!>())
}
