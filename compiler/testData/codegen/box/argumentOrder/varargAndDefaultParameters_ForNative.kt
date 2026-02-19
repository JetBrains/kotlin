// NO_CHECK_LAMBDA_INLINING
// WITH_STDLIB
// TARGET_BACKEND: NATIVE
// FILE: 1.kt
package test

open class A(val value: String)

var invokeOrder = ""

inline fun inlineFun(
        vararg constraints: A,
        receiver: String = { invokeOrder += " default receiver"; "DEFAULT" }(),
        init: String
): String {
    return constraints.map { it.value }.joinToString() + ", " + receiver + ", " + init
}

// FILE: 2.kt
import test.*


var result = ""
fun box(): String {

    result = ""
    invokeOrder = ""
    result = inlineFun(constraints = *arrayOf({ invokeOrder += "constraints";A("C") }()),
                       receiver = { invokeOrder += " receiver"; "R" }(),
                       init = { invokeOrder += " init"; "I" }())
    if (result != "C, R, I") return "fail 1: $result"

    if (invokeOrder != "constraints receiver init") return "fail 2: $invokeOrder"

    result = ""
    invokeOrder = ""
    result = inlineFun(init = { invokeOrder += "init"; "I" }(),
                       constraints = *arrayOf({ invokeOrder += " constraints";A("C") }()),
                       receiver = { invokeOrder += " receiver"; "R" }()
    )
    if (result != "C, R, I") return "fail 3: $result"
    if (invokeOrder != "init constraints receiver") return "fail 4: $invokeOrder"

    result = ""
    invokeOrder = ""
    result = inlineFun(init = { invokeOrder += "init"; "I" }(),
                       constraints = *arrayOf({ invokeOrder += " constraints";A("C") }()))
    if (result != "C, DEFAULT, I") return "fail 5: $result"
    if (invokeOrder != "init constraints default receiver") return "fail 6: $invokeOrder"

    return "OK"
}

