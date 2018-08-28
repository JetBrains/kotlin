// IGNORE_BACKEND: JVM_IR
// FILE: 1.kt

package test

inline fun test(a: Int, b: Long, crossinline c: () -> String): String {
    return { "${a}_${b}_${c()}"} ()
}

// FILE: 2.kt

import test.*

fun box(): String {
    var invokeOrder = "";
    val expectedResult = "0_1_9"
    val expectedInvokeOrder = "1_0_9"
    var l = 1L
    var i = 0
    val captured = 9L

    var result = test(b = {invokeOrder += "1_"; l}(), a = {invokeOrder+="0_"; i}(), c = {invokeOrder += "$captured"; "$captured"})
    if (invokeOrder != expectedInvokeOrder || result != expectedResult) return "fail 1: $invokeOrder != $expectedInvokeOrder or $result != $expectedResult"

    invokeOrder = "";
    result = test(b = {invokeOrder += "1_"; l}(), c = {invokeOrder += "$captured"; "$captured"}, a = {invokeOrder+="0_"; i}())
    if (invokeOrder != expectedInvokeOrder || result != expectedResult) return "fail 2: $invokeOrder != $expectedInvokeOrder or $result != $expectedResult"


    invokeOrder = "";
    result = test(c = {invokeOrder += "$captured"; "$captured"}, b = {invokeOrder += "1_"; l}(), a = {invokeOrder+="0_"; i}())
    if (invokeOrder != expectedInvokeOrder || result != expectedResult) return "fail 3: $invokeOrder != $expectedInvokeOrder or $result != $expectedResult"


    invokeOrder = "";
    result = test(a = {invokeOrder+="0_"; i}(), c = {invokeOrder += "$captured"; "$captured"}, b = {invokeOrder += "1_"; l}())
    if (invokeOrder != "0_1_9" || result != expectedResult) return "fail 4: $invokeOrder != 0_1_9 or $result != $expectedResult"

    return "OK"
}
