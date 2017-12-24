// !WITH_NEW_INFERENCE
// !CHECK_TYPE

package a

fun <T> emptyList(): List<T> = throw Exception()

fun <T> foo(f: T.() -> Unit, l: List<T>): T = throw Exception("$f$l")

fun test() {
    val q = foo(fun Int.() {}, emptyList()) //type inference no information for parameter error
    checkSubtype<Int>(q)

    <!OI;TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>foo<!>({}, <!OI;TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>emptyList<!>())
}