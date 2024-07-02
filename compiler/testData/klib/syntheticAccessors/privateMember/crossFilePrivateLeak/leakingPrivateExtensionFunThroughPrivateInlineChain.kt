// IGNORE_BACKEND: NATIVE

// FILE: A.kt
class A {
    internal inline fun internalInlineExtension() = privateInlineExtension4() + "1"
}

private fun A.privateExtension() = "OK"
private inline fun A.privateInlineExtension1() = privateExtension()
private inline fun A.privateInlineExtension2() = privateInlineExtension1()
private inline fun A.privateInlineExtension3() = privateInlineExtension2()
private inline fun A.privateInlineExtension4() = privateInlineExtension3()

internal fun topLevelFun() = A().internalInlineExtension() + "2"

internal inline fun topLevelInlineFun() = A().internalInlineExtension() + "3"

// FILE: main.kt
fun box(): String {
    var result = ""
    result += A().internalInlineExtension()
    result += " "
    result += topLevelFun()
    result += " "
    result += topLevelInlineFun()
    if (result != "OK1 OK12 OK13") return result
    return "OK"
}
