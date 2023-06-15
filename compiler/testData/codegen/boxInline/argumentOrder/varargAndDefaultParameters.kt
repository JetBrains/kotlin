// IGNORE_BACKEND: WASM
// WITH_STDLIB
// !LANGUAGE: -UseCorrectExecutionOrderForVarargArguments
// IGNORE_BACKEND: JVM_IR
// IGNORE_BACKEND: JS, JS_IR
// IGNORE_BACKEND: JS_IR_ES6
// IGNORE_BACKEND_MULTI_MODULE: JVM_IR, JVM_MULTI_MODULE_IR_AGAINST_OLD
// IGNORE_BACKEND_K2_MULTI_MODULE: JVM_IR JVM_IR_SERIALIZE
// NO_CHECK_LAMBDA_INLINING
// KJS_WITH_FULL_RUNTIME
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

    if (invokeOrder != " receiver initconstraints") return "fail 2: $invokeOrder"

    result = ""
    invokeOrder = ""
    result = inlineFun(init = { invokeOrder += "init"; "I" }(),
                       constraints = *arrayOf({ invokeOrder += " constraints";A("C") }()),
                       receiver = { invokeOrder += " receiver"; "R" }()
    )
    if (result != "C, R, I") return "fail 3: $result"
    if (invokeOrder != "init receiver constraints") return "fail 4: $invokeOrder"

    result = ""
    invokeOrder = ""
    result = inlineFun(init = { invokeOrder += "init"; "I" }(),
                       constraints = *arrayOf({ invokeOrder += " constraints";A("C") }()))
    if (result != "C, DEFAULT, I") return "fail 5: $result"
    if (invokeOrder != "init constraints default receiver") return "fail 6: $invokeOrder"

    return "OK"
}
