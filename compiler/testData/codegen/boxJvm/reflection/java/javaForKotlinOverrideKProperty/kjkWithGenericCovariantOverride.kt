// TARGET_BACKEND: JVM
// WITH_REFLECT
// FILE: test/JMid.java
package test;

public abstract class JMid<T> extends KBase {
    @Override
    public T getValue() {
        return null;
    }
}

// FILE: test/box.kt
package test

import kotlin.test.assertEquals
import kotlin.reflect.full.memberProperties

abstract class KBase {
    abstract val value: Any?
}

class Concrete : JMid<String>()

fun box(): String {
    val property = Concrete::class.memberProperties.single { it.name == "value" }
    assertEquals(String::class, property.returnType.classifier)

    return "OK"
}
