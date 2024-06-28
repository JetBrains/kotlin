// TARGET_BACKEND: JVM_IR
// LANGUAGE: +ReferencesToSyntheticJavaProperties
// WITH_REFLECT

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

fun box(): String {
    val stringProperty = J::stringProperty
    assertEquals("property stringProperty (Kotlin reflection is not available)", stringProperty.toString())
    try {
        stringProperty.visibility
        return "Fail"
    } catch (e: UnsupportedOperationException) {
        assertEquals("Kotlin reflection is not yet supported for synthetic Java properties. " +
                             "Please follow/upvote https://youtrack.jetbrains.com/issue/KT-55980", e.message)
        return "OK"
    }
}