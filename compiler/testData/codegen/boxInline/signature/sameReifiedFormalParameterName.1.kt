// NO_CHECK_LAMBDA_INLINING
// FULL_JDK

import test.*
import java.util.*


fun box(): String {
    val result = Test().callInline()
    val method = result.javaClass.getMethod("aTest", Any::class.java)
    val genericReturnType = method.genericReturnType
    if (genericReturnType.toString() != "test.B<T>") return "fail 1: ${genericReturnType}"

    val genericParameterTypes = method.genericParameterTypes
    if (genericParameterTypes.size != 1) return "fail 2: ${genericParameterTypes.size}"
    if (genericParameterTypes[0].toString() != "T") return "fail 3: ${genericParameterTypes[0]}"

    return "OK"
}
