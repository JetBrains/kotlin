// TARGET_BACKEND: JVM_IR
// !LANGUAGE: +ReferencesToSyntheticJavaProperties
// WITH_REFLECT
// WITH_COROUTINES

// FILE: J.java

public class J {
    private String stringProperty;

    public String getStringProperty() {
        return stringProperty;
    }

    public void setStringProperty(String value) {
        stringProperty = value;
    }
}

// FILE: main.kt

import kotlin.reflect.*
import kotlin.test.*
import kotlin.reflect.full.callSuspend
import helpers.*
import kotlin.coroutines.startCoroutine

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}


fun box(): String {
    val stringProperty = J::stringProperty
    assertEquals("property stringProperty (Kotlin reflection is not available)", stringProperty.toString())
    assertEquals("stringProperty", stringProperty.name)
    assertEquals("getter of property stringProperty (Kotlin reflection is not available)", stringProperty.getter.toString())
    assertEquals("get-stringProperty", stringProperty.getter.name)
    assertEquals("setter of property stringProperty (Kotlin reflection is not available)", stringProperty.setter.toString())
    assertEquals("set-stringProperty", stringProperty.setter.name)

    assertFailsWith(UnsupportedOperationException::class) { stringProperty.visibility }
    assertFailsWith(UnsupportedOperationException::class) { stringProperty.callBy(mapOf()) }
    assertFailsWith(UnsupportedOperationException::class) { stringProperty.getter.callBy(mapOf()) }
    assertFailsWith(UnsupportedOperationException::class) { stringProperty.setter.callBy(mapOf()) }
    assertFailsWith(UnsupportedOperationException::class) { stringProperty.getter.returnType }
    assertFailsWith(UnsupportedOperationException::class) { stringProperty.isSuspend }
    assertFailsWith(UnsupportedOperationException::class) { stringProperty.getter.isSuspend }
    assertFailsWith(UnsupportedOperationException::class) { stringProperty.setter.isSuspend }
    builder {
        assertFailsWith(UnsupportedOperationException::class) { stringProperty.callSuspend(J()) }
        assertFailsWith(UnsupportedOperationException::class) { stringProperty.getter.callSuspend(J()) }
        assertFailsWith(UnsupportedOperationException::class) { stringProperty.setter.callSuspend(J(), "") }
    }
    return "OK"
}