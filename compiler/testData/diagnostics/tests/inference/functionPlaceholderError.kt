package a

fun <T> emptyList(): List<T> = throw Exception()

fun foo<T>(f: T.() -> Unit, l: List<T>): T = throw Exception("$f$l")

fun test() {
    val q = foo({ Int.() -> }, emptyList()) //type inference no information for parameter error
    q: Int

    <!TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>foo<!>({}, <!TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>emptyList<!>())
}