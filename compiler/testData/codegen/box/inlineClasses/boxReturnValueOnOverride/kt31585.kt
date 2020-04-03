// IGNORE_BACKEND_FIR: JVM_IR
// WITH_RUNTIME
// KJS_WITH_FULL_RUNTIME

inline class FieldValue(val value: String)

enum class RequestFields {
    ENUM_ONE
}

data class RequestInputParameters(
    private val backingMap: Map<RequestFields, FieldValue>
) : Map<RequestFields, FieldValue> by backingMap

fun box(): String {
    val testMap1 = mapOf(RequestFields.ENUM_ONE to FieldValue("value1"))
    val test1 = testMap1[RequestFields.ENUM_ONE]!!
    if (test1.value != "value1") throw AssertionError("test1: $test1")

    val testMap2 = RequestInputParameters(mapOf(RequestFields.ENUM_ONE to FieldValue("value2")))
    val test2 = testMap2[RequestFields.ENUM_ONE]!!
    if (test2.value != "value2") throw AssertionError("test2: $test2")

    return "OK"
}