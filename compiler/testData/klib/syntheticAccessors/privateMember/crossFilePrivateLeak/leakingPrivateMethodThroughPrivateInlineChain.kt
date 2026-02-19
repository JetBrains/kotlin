// FILE: A.kt
class A {
    private fun privateMethod() = "OK"
    private inline fun privateInlineMethod1() = privateMethod()
    private inline fun privateInlineMethod2() = privateInlineMethod1()
    private inline fun privateInlineMethod3() = privateInlineMethod2()
    private inline fun privateInlineMethod4() = privateInlineMethod3()
    internal inline fun internalInlineMethod() = privateInlineMethod4() + "1"
}

internal fun topLevelFun() = A().internalInlineMethod() + "2"

internal inline fun topLevelInlineFun() = A().internalInlineMethod() + "3"

// FILE: main.kt
fun box(): String {
    var result = ""
    result += A().internalInlineMethod()
    result += " "
    result += topLevelFun()
    result += " "
    result += topLevelInlineFun()
    if (result != "OK1 OK12 OK13") return result
    return "OK"
}
