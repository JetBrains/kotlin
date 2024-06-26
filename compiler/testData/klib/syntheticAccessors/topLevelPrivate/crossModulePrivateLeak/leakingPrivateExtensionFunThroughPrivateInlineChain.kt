// MODULE: lib
// FILE: a.kt
private fun String.privateExtension() = "OK"
private inline fun String.privateInlineExtension1() = privateExtension()
private inline fun String.privateInlineExtension2() = privateInlineExtension1()
private inline fun String.privateInlineExtension3() = privateInlineExtension2()
private inline fun String.privateInlineExtension4() = privateInlineExtension3()
internal inline fun String.internalInlineExtension() = privateInlineExtension4() + "1"

internal fun topLevelFun() = "".internalInlineExtension() + "2"

internal inline fun topLevelInlineFun() = "".internalInlineExtension() + "3"

// MODULE: main()(lib)
// FILE: main.kt
fun box(): String {
    var result = ""
    result += "".internalInlineExtension()
    result += " "
    result += topLevelFun()
    result += " "
    result += topLevelInlineFun()
    if (result != "OK1 OK12 OK13") return result
    return "OK"
}
