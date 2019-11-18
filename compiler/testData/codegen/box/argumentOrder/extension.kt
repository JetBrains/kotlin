// IGNORE_BACKEND_FIR: JVM_IR
fun box(): String {
    var invokeOrder = "";
    val expectedResult = "1_0_1_L"
    val expectedInvokeOrder = "1_0_L"
    var l = 1L
    var i = 0

    var result = 1.0.test(b = {invokeOrder += "1_"; l}(), a = {invokeOrder+="0_"; i}(), c = {invokeOrder += "L"; "L"})
    if (invokeOrder != expectedInvokeOrder || result != expectedResult) return "fail 1: $invokeOrder != $expectedInvokeOrder or $result != $expectedResult"

    invokeOrder = "";
    result = 1.0.test(b = {invokeOrder += "1_"; l}(), c = {invokeOrder += "L"; "L"}, a = {invokeOrder+="0_"; i}())
    if (invokeOrder != expectedInvokeOrder || result != expectedResult) return "fail 2: $invokeOrder != $expectedInvokeOrder or $result != $expectedResult"


    invokeOrder = "";
    result = 1.0.test(c = {invokeOrder += "L"; "L"}, b = {invokeOrder += "1_"; l}(), a = {invokeOrder+="0_"; i}())
    if (invokeOrder != expectedInvokeOrder || result != expectedResult) return "fail 3: $invokeOrder != $expectedInvokeOrder or $result != $expectedResult"


    invokeOrder = "";
    result = 1.0.test(a = {invokeOrder+="0_"; i}(), c = {invokeOrder += "L"; "L"}, b = {invokeOrder += "1_"; l}())
    if (invokeOrder != "0_1_L" || result != expectedResult) return "fail 4: $invokeOrder != 0_1_L or $result != $expectedResult"

    return "OK"
}

fun Double.test(a: Int, b: Long, c: () -> String): String {
    return "${this.toInt()}_${a}_${b}_${c()}"
}