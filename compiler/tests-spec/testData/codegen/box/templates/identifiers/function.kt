<!DIRECTIVES("HELPERS: REFLECT")!>

fun <!ELEMENT(1)!>(): Boolean {
    return true
}

fun Boolean.<!ELEMENT(2)!>() = true

fun <T> T.<!ELEMENT(4)!>() = listOf(0).<!ELEMENT(3)!>()

inline fun <reified T, K> K?.<!ELEMENT(3)!>(x1: T = 10 as T)
        where K : List<T>,
              K : Iterable<T>,
              T : Comparable<T>,
              T : Number = false

fun box(): String? {
    if (!<!ELEMENT(1)!>()) return null
    if (null.<!ELEMENT(3)!><Int, List<Int>>()) return null
    if (false.<!ELEMENT(4)!>()) return null
    if (!false.<!ELEMENT(2)!>()) return null
    if (Any().<!ELEMENT(4)!>()) return null

    if (!checkFunctionName(::<!ELEMENT(1)!>, "<!ELEMENT_VALIDATION(1)!>")) return null
    if (!checkFunctionName(Boolean::<!ELEMENT(2)!>, "<!ELEMENT_VALIDATION(2)!>")) return null
    if (!checkFunctionName(Boolean::<!ELEMENT(4)!>, "<!ELEMENT_VALIDATION(4)!>")) return null
    if (!checkFunctionName(List<Int>::<!ELEMENT(3)!>, "<!ELEMENT_VALIDATION(3)!>")) return null
    if (!checkFunctionName(Nothing?::<!ELEMENT(4)!>, "<!ELEMENT_VALIDATION(4)!>")) return null

    return "OK"
}
