// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM

// WITH_REFLECT
// KT-8131 Cannot find backing field in ancestor class via reflection

import kotlin.reflect.*
import kotlin.reflect.full.*
import kotlin.reflect.jvm.*

open class TestBase {
    var id = 0L
}

class TestChild : TestBase()

fun box(): String {
    val property = TestChild::class.memberProperties.first { it.name == "id" } as KMutableProperty<*>
    if (property.javaField == null)
        return "Fail: no field"
    if (property.javaGetter == null)
        return "Fail: no getter"
    if (property.javaSetter == null)
        return "Fail: no setter"

    return "OK"
}
