annotation class A

fun box(): String? {
    var value_1 = false
    var value_2 = true

    try {
        throw Exception()
    } catch (<!ELEMENT!>: Throwable) {
        value_1 = true
    }

    try {
        throw Exception()
    } catch (@A <!ELEMENT!>: Throwable) {
        value_2 = false
    }

    if (!value_1 || value_2) return null

    return "OK"
}