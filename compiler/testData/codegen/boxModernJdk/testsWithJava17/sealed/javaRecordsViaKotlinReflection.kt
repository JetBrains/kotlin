// WITH_REFLECT
// ISSUE: KT-47760

// FILE: MyRecord.java
public record MyRecord(String stringField) {}

// FILE: main.kt
import kotlin.reflect.full.*
import kotlin.reflect.KVisibility
import kotlin.reflect.jvm.isAccessible

fun box(): String {
    val expectedValue = "Hello"
    val obj = MyRecord(expectedValue)

    // stringField() function
    val function = MyRecord::class.functions.single { it.name == "stringField" }
    val functionValue = function.call(obj)
    if (functionValue != expectedValue) {
        return "Fail: stringField() call returned $functionValue, expected $expectedValue"
    }

    // stringField field
    val property = MyRecord::class.memberProperties.single { it.name == "stringField" }
    if (property.visibility != KVisibility.PRIVATE) {
        return "Fail: field stringField is not private"
    }
    val getter = property.getter
    getter.isAccessible = true
    val propertyValue = getter.call(obj)
    if (propertyValue != expectedValue) {
        return "Fail: stringField field returned $propertyValue, expected $expectedValue"
    }

    return "OK"
}
