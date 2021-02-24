// !LANGUAGE: +BareArrayClassLiteral

fun box(): String {
    val x = Array(1) { Any() }
    if (x::class != Array::class) return "Fail"

    return "OK"
}
