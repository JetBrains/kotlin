// TARGET_BACKEND: JVM_IR
// !LANGUAGE: +ReferencesToSyntheticJavaProperties
// WITH_STDLIB

// FILE: J.java

public class J {
    private String stringProperty;
    private boolean myBooleanProperty;
    public int numGetCalls;
    public int numSetCalls;

    public String getStringProperty() {
        numGetCalls++;
        return stringProperty;
    }

    public void setStringProperty(String value) {
        numSetCalls++;
        stringProperty = value;
    }

    public boolean isBooleanProperty() {
        numGetCalls++;
        return myBooleanProperty;
    }

    public void setBooleanProperty(boolean value) {
        numSetCalls++;
        myBooleanProperty = value;
    }
}

// FILE: main.kt

import kotlin.reflect.*
import kotlin.test.*

fun box(): String {
    val j = J()

    val unboundStringProperty = J::stringProperty
    assertNull(unboundStringProperty.get(j))
    unboundStringProperty.set(j, "Hi")
    assertEquals("Hi", unboundStringProperty.get(j))
    assertEquals("Hi", unboundStringProperty(j))

    assertEquals(3, j.numGetCalls)
    assertEquals(1, j.numSetCalls)

    val boundStringProperty = j::stringProperty
    assertEquals("Hi", boundStringProperty.get())
    boundStringProperty.set("Hello")
    assertEquals("Hello", boundStringProperty.get())
    assertEquals("Hello", boundStringProperty())

    assertEquals(6, j.numGetCalls)
    assertEquals(2, j.numSetCalls)

    val unboundBooleanProperty: KMutableProperty1<J, Boolean> = J::isBooleanProperty
    assertFalse(unboundBooleanProperty.get(j))
    unboundBooleanProperty.set(j, true)
    assertTrue(unboundBooleanProperty.get(j))
    assertTrue(unboundBooleanProperty(j))
    assertTrue(unboundBooleanProperty.call(j))

    assertEquals(10, j.numGetCalls)
    assertEquals(3, j.numSetCalls)

    val boundBooleanProperty: KMutableProperty0<Boolean> = j::isBooleanProperty
    assertTrue(boundBooleanProperty.get())
    boundBooleanProperty.set(false)
    assertFalse(boundBooleanProperty.get())
    assertFalse(boundBooleanProperty())
    assertFalse(boundBooleanProperty.call())

    assertEquals(14, j.numGetCalls)
    assertEquals(4, j.numSetCalls)

    unboundBooleanProperty.setter(j, true)
    assertTrue(unboundBooleanProperty.getter(j))
    assertEquals(15, j.numGetCalls)
    assertEquals(5, j.numSetCalls)

    unboundBooleanProperty.setter.call(j, false)
    assertFalse(unboundBooleanProperty.getter.call(j))
    assertEquals(16, j.numGetCalls)
    assertEquals(6, j.numSetCalls)

    boundBooleanProperty.setter(false)
    assertEquals(7, j.numSetCalls)
    assertFalse(boundBooleanProperty.getter())
    assertEquals(17, j.numGetCalls)

    boundBooleanProperty.setter.call(true)
    assertEquals(8, j.numSetCalls)
    assertTrue(boundBooleanProperty.getter.call())
    assertEquals(18, j.numGetCalls)

    return "OK"
}