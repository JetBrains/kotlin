// TARGET_BACKEND: JVM_IR
// IGNORE_LIGHT_ANALYSIS
// JVM_ABI_K1_K2_DIFF: KT-63960
// LANGUAGE: +ReferencesToSyntheticJavaProperties
// LANGUAGE: -SkipStandaloneScriptsInSourceRoots
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

fun runScriptMethod(name: String, method: String): Any {
    val klass = Thread.currentThread().contextClassLoader.loadClass(name)
    val constructor = klass.constructors.single()
    val instance = constructor.newInstance(emptyArray<String>())
    val method = klass.getMethod(method)
    return method.invoke(instance)
}

fun box(): String =
    runScriptMethod("Script", "f") as String

// FILE: script.kts

import kotlin.reflect.*
import kotlin.test.*

fun f(): String {
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

    assertEquals(9, j.numGetCalls)
    assertEquals(3, j.numSetCalls)

    val boundBooleanProperty: KMutableProperty0<Boolean> = j::isBooleanProperty
    assertTrue(boundBooleanProperty.get())
    boundBooleanProperty.set(false)
    assertFalse(boundBooleanProperty.get())
    assertFalse(boundBooleanProperty())

    assertEquals(12, j.numGetCalls)
    assertEquals(4, j.numSetCalls)

    return "OK"
}
